package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.events.PaymentRequested
import com.caracolas.gestortrainer.core.events.PaymentSucceeded
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PaymentRequestId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.payments.api.BillingPeriod
import com.caracolas.gestortrainer.feature.payments.api.CheckoutRequest
import com.caracolas.gestortrainer.feature.payments.api.Delivery
import com.caracolas.gestortrainer.feature.payments.api.Entitlement
import com.caracolas.gestortrainer.feature.payments.api.EntitlementService
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequest
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestError
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestException
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestService
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestService.Companion.EXPIRES_AFTER_DAYS
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestService.Companion.MAX_AMOUNT_MINOR
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestStatus
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
import com.caracolas.gestortrainer.feature.payments.api.PaymentRouter
import com.caracolas.gestortrainer.feature.payments.api.Product
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Solicitudes de pago y lo que cada usuario tiene pagado, en memoria mientras no exista el backend. */
@Singleton
class InMemoryPaymentRequestService @Inject constructor(
    private val linkService: LinkService,
    private val profileRepository: ProfileRepository,
    private val paymentRouter: PaymentRouter,
    private val eventBus: EventBus,
) : PaymentRequestService, EntitlementService {
    private val requests = MutableStateFlow<Map<PaymentRequestId, PaymentRequest>>(emptyMap())
    private val entitlements = MutableStateFlow<Map<UserId, List<Entitlement>>>(emptyMap())

    override fun observe(userId: UserId): Flow<List<Entitlement>> = entitlements.map { it[userId].orEmpty() }

    override suspend fun saveDraft(
        trainerId: TrainerId,
        clientPublicId: PublicId,
        period: BillingPeriod,
        amount: Money,
    ): Result<PaymentRequestId> {
        if (amount.amountMinor <= 0 || amount.amountMinor > MAX_AMOUNT_MINOR) {
            return Result.failure(PaymentRequestException(PaymentRequestError.INVALID_AMOUNT))
        }
        val client = profileRepository.findClientByPublicId(clientPublicId)
            ?: return Result.failure(PaymentRequestException(PaymentRequestError.CLIENT_NOT_LINKED))
        val clientId = ClientId(client.userId.value)
        val linkedTrainers = linkService.observeLinkedTrainers(clientId).first()
        if (trainerId !in linkedTrainers) {
            return Result.failure(PaymentRequestException(PaymentRequestError.CLIENT_NOT_LINKED))
        }

        val id = PaymentRequestId(UUID.randomUUID().toString())
        val draft = PaymentRequest(id, trainerId, clientId, period, amount, PaymentRequestStatus.DRAFT, Instant.now(), expiresAt = null)
        requests.value = requests.value + (id to draft)
        return Result.success(id)
    }

    override suspend fun send(id: PaymentRequestId): Result<Unit> {
        val request = requests.value[id]?.withLazyExpiry()
            ?: return Result.failure(PaymentRequestException(PaymentRequestError.NOT_FOUND))

        var updated = requests.value
        updated.values
            .filter { it.trainerId == request.trainerId && it.clientId == request.clientId && it.id != id && it.withLazyExpiry().status == PaymentRequestStatus.SENT }
            .forEach { updated = updated + (it.id to it.copy(status = PaymentRequestStatus.CANCELLED)) }

        val now = Instant.now()
        val sent = request.copy(status = PaymentRequestStatus.SENT, createdAt = request.createdAt, expiresAt = now.plus(EXPIRES_AFTER_DAYS, ChronoUnit.DAYS))
        updated = updated + (id to sent)
        requests.value = updated
        eventBus.publish(PaymentRequested(id, sent.trainerId, sent.clientId, sent.amount, now))
        return Result.success(Unit)
    }

    override suspend fun cancel(id: PaymentRequestId): Result<Unit> {
        val request = requests.value[id] ?: return Result.failure(PaymentRequestException(PaymentRequestError.NOT_FOUND))
        requests.value = requests.value + (id to request.copy(status = PaymentRequestStatus.CANCELLED))
        return Result.success(Unit)
    }

    override fun observeForTrainer(trainerId: TrainerId): Flow<List<PaymentRequest>> = requests.map { all ->
        all.values.filter { it.trainerId == trainerId }.map { it.withLazyExpiry() }
    }

    override fun observePendingForClient(clientId: ClientId): Flow<List<PaymentRequest>> = requests.map { all ->
        all.values.map { it.withLazyExpiry() }.filter { it.clientId == clientId && it.status == PaymentRequestStatus.SENT }
    }

    override suspend fun pay(id: PaymentRequestId): PaymentResult {
        val request = requests.value[id]?.withLazyExpiry()
            ?: return PaymentResult.Failed("No existe la solicitud de pago.")
        if (request.status != PaymentRequestStatus.SENT) {
            return PaymentResult.Failed("Esta solicitud ya no está disponible.")
        }

        val region = profileRepository.observeClient(request.clientId).first()?.personal?.region ?: Region("ES")
        val product = Product.OneOffRequest(id, request.trainerId)
        val gateway = paymentRouter.gatewayFor(product, region, Delivery.ONLINE)
        val result = gateway.checkout(CheckoutRequest(UserId(request.clientId.value), product, region, Delivery.ONLINE))

        if (result is PaymentResult.Success) {
            requests.value = requests.value + (id to request.copy(status = PaymentRequestStatus.PAID))
            val userId = UserId(request.clientId.value)
            val expiresAt = Instant.now().plus(request.period.days.toLong(), ChronoUnit.DAYS)
            val entitlement = Entitlement(productKey = "trainer:${request.trainerId.value}", expiresAt = expiresAt)
            entitlements.value = entitlements.value + (userId to (entitlements.value[userId].orEmpty() + entitlement))
            eventBus.publish(PaymentSucceeded(userId, result.paymentId, request.amount, Instant.now()))
        }
        return result
    }

    private fun PaymentRequest.withLazyExpiry(): PaymentRequest {
        val expiry = expiresAt
        return if (status == PaymentRequestStatus.SENT && expiry != null && expiry.isBefore(Instant.now())) {
            copy(status = PaymentRequestStatus.EXPIRED)
        } else {
            this
        }
    }
}
