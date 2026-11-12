package com.caracolas.gestortrainer.feature.matching.impl

import com.caracolas.gestortrainer.core.events.ClientLinked
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.TrainerRequestId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.matching.api.MatchingError
import com.caracolas.gestortrainer.feature.matching.api.MatchingException
import com.caracolas.gestortrainer.feature.matching.api.MatchingService
import com.caracolas.gestortrainer.feature.matching.api.Proposal
import com.caracolas.gestortrainer.feature.matching.api.RequestGoal
import com.caracolas.gestortrainer.feature.matching.api.ShareFlags
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequest
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequestStatus
import com.caracolas.gestortrainer.feature.payments.api.CheckoutRequest
import com.caracolas.gestortrainer.feature.payments.api.Delivery
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
import com.caracolas.gestortrainer.feature.payments.api.PaymentRouter
import com.caracolas.gestortrainer.feature.payments.api.Product
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Pipeline solicitud -> propuesta -> pago en memoria mientras no exista el backend. */
@Singleton
class InMemoryMatchingService @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val paymentRouter: PaymentRouter,
    private val eventBus: EventBus,
) : MatchingService {
    private val requests = MutableStateFlow<Map<TrainerRequestId, TrainerRequest>>(emptyMap())

    override suspend fun sendRequest(
        clientId: ClientId,
        trainerPublicId: PublicId,
        message: String,
        goals: List<RequestGoal>,
        shareFlags: ShareFlags,
    ): Result<TrainerRequestId> {
        val trainer = profileRepository.findTrainerByPublicId(trainerPublicId)
            ?: return Result.failure(MatchingException(MatchingError.TRAINER_NOT_FOUND))

        val id = TrainerRequestId(UUID.randomUUID().toString())
        val request = TrainerRequest(
            id = id,
            trainerId = TrainerId(trainer.userId.value),
            clientId = clientId,
            message = message.trim(),
            goals = goals,
            shareFlags = shareFlags,
            status = TrainerRequestStatus.NEW,
            proposal = null,
            createdAt = Instant.now(),
        )
        requests.value = requests.value + (id to request)
        return Result.success(id)
    }

    override fun observeForTrainer(trainerId: TrainerId): Flow<List<TrainerRequest>> = requests.map { all ->
        all.values.filter { it.trainerId == trainerId }.sortedByDescending { it.createdAt }
    }

    override fun observeForClient(clientId: ClientId): Flow<List<TrainerRequest>> = requests.map { all ->
        all.values.filter { it.clientId == clientId }.sortedByDescending { it.createdAt }
    }

    override suspend fun reject(requestId: TrainerRequestId): Result<Unit> {
        val request = requests.value[requestId]
            ?: return Result.failure(MatchingException(MatchingError.REQUEST_NOT_FOUND))
        requests.value = requests.value + (requestId to request.copy(status = TrainerRequestStatus.REJECTED))
        return Result.success(Unit)
    }

    override suspend fun sendProposal(requestId: TrainerRequestId, proposal: Proposal): Result<Unit> {
        val request = requests.value[requestId]
            ?: return Result.failure(MatchingException(MatchingError.REQUEST_NOT_FOUND))
        if (request.status != TrainerRequestStatus.NEW) {
            return Result.failure(MatchingException(MatchingError.NOT_IN_NEW_STATUS))
        }
        requests.value = requests.value + (requestId to request.copy(status = TrainerRequestStatus.PROPOSAL_SENT, proposal = proposal))
        return Result.success(Unit)
    }

    override suspend fun acceptAndPay(requestId: TrainerRequestId): PaymentResult {
        val request = requests.value[requestId]
            ?: return PaymentResult.Failed("Solicitud no encontrada")
        val proposal = request.proposal
            ?: return PaymentResult.Failed("Esta solicitud no tiene propuesta")
        if (request.status != TrainerRequestStatus.PROPOSAL_SENT) {
            return PaymentResult.Failed("La propuesta ya no está disponible")
        }
        if (proposal.expiresAt.isBefore(Instant.now())) {
            return PaymentResult.Failed("La propuesta ha caducado")
        }

        val trainerProfile = profileRepository.observeTrainer(request.trainerId).first()
        val region = trainerProfile?.personal?.region ?: Region("ES")
        val product = Product.MatchingProposal(requestId, request.trainerId)
        val gateway = paymentRouter.gatewayFor(product, region, Delivery.ONLINE)
        val result = gateway.checkout(
            CheckoutRequest(userId = UserId(request.clientId.value), product = product, region = region, delivery = Delivery.ONLINE),
        )
        if (result is PaymentResult.Success) {
            requests.value = requests.value + (requestId to request.copy(status = TrainerRequestStatus.ACCEPTED))
            eventBus.publish(ClientLinked(request.trainerId, request.clientId, Instant.now()))
        }
        return result
    }

    override suspend fun updateShareFlags(requestId: TrainerRequestId, flags: ShareFlags): Result<Unit> {
        val request = requests.value[requestId]
            ?: return Result.failure(MatchingException(MatchingError.REQUEST_NOT_FOUND))
        requests.value = requests.value + (requestId to request.copy(shareFlags = flags))
        return Result.success(Unit)
    }
}
