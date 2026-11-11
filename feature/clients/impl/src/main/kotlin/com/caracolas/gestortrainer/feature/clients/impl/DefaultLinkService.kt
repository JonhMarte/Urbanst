package com.caracolas.gestortrainer.feature.clients.impl

import com.caracolas.gestortrainer.core.events.ClientLinked
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.events.LinkRequested
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.LinkRequestId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.clients.api.LinkDirection
import com.caracolas.gestortrainer.feature.clients.api.LinkError
import com.caracolas.gestortrainer.feature.clients.api.LinkException
import com.caracolas.gestortrainer.feature.clients.api.LinkRequest
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.clients.api.LinkStatus
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Vinculación trainer-alumno en memoria (Fase 1, sin backend). */
@Singleton
class DefaultLinkService @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val eventBus: EventBus,
) : LinkService {
    private val requests = MutableStateFlow<List<LinkRequest>>(emptyList())

    override suspend fun requestByTrainerId(clientId: ClientId, trainerPublicId: PublicId): Result<LinkRequestId> {
        val trainer = profileRepository.findTrainerByPublicId(trainerPublicId)
            ?: return Result.failure(LinkException(LinkError.TRAINER_NOT_FOUND))
        return createRequest(TrainerId(trainer.userId.value), clientId, LinkDirection.CLIENT_TO_TRAINER)
    }

    override suspend fun addByClientId(trainerId: TrainerId, clientPublicId: PublicId): Result<LinkRequestId> {
        val client = profileRepository.findClientByPublicId(clientPublicId)
            ?: return Result.failure(LinkException(LinkError.CLIENT_NOT_FOUND))
        return createRequest(trainerId, ClientId(client.userId.value), LinkDirection.TRAINER_TO_CLIENT)
    }

    override fun observeIncoming(userId: UserId): Flow<List<LinkRequest>> = requests.map { list ->
        list.filter { it.status == LinkStatus.PENDING && isRecipient(it, userId) }
    }

    override suspend fun accept(requestId: LinkRequestId): Result<Unit> =
        resolve(requestId, LinkStatus.ACCEPTED) { request, now ->
            eventBus.publish(ClientLinked(request.trainerId, request.clientId, now))
        }

    override suspend fun reject(requestId: LinkRequestId): Result<Unit> = resolve(requestId, LinkStatus.REJECTED)

    /** Alumnos ya vinculados de un trainer; lo usa [DefaultClientRosterRepository] dentro de este mismo módulo. */
    fun acceptedLinksFor(trainerId: TrainerId): Flow<List<ClientId>> = requests.map { list ->
        list.filter { it.trainerId == trainerId && it.status == LinkStatus.ACCEPTED }.map { it.clientId }
    }

    override fun observeLinkedTrainers(clientId: ClientId): Flow<List<TrainerId>> = requests.map { list ->
        list.filter { it.clientId == clientId && it.status == LinkStatus.ACCEPTED }.map { it.trainerId }
    }

    private suspend fun createRequest(
        trainerId: TrainerId,
        clientId: ClientId,
        direction: LinkDirection,
    ): Result<LinkRequestId> {
        val existing = requests.value.filter { it.trainerId == trainerId && it.clientId == clientId }
        if (existing.any { it.status == LinkStatus.ACCEPTED }) {
            return Result.failure(LinkException(LinkError.ALREADY_LINKED))
        }
        if (existing.any { it.status == LinkStatus.PENDING }) {
            return Result.failure(LinkException(LinkError.REQUEST_ALREADY_PENDING))
        }

        val id = LinkRequestId(UUID.randomUUID().toString())
        val now = Instant.now()
        requests.value = requests.value + LinkRequest(id, trainerId, clientId, direction, LinkStatus.PENDING, now)
        eventBus.publish(LinkRequested(trainerId, clientId, now))
        return Result.success(id)
    }

    private suspend fun resolve(
        requestId: LinkRequestId,
        newStatus: LinkStatus,
        onResolved: suspend (LinkRequest, Instant) -> Unit = { _, _ -> },
    ): Result<Unit> {
        val current = requests.value.find { it.id == requestId }
            ?: return Result.failure(NoSuchElementException("No existe la solicitud $requestId"))
        if (current.status != LinkStatus.PENDING) {
            return Result.failure(IllegalStateException("La solicitud ya se había resuelto"))
        }
        val now = Instant.now()
        requests.value = requests.value.map { if (it.id == requestId) it.copy(status = newStatus) else it }
        onResolved(current, now)
        return Result.success(Unit)
    }

    private fun isRecipient(request: LinkRequest, userId: UserId): Boolean = when (request.direction) {
        LinkDirection.CLIENT_TO_TRAINER -> request.trainerId.value == userId.value
        LinkDirection.TRAINER_TO_CLIENT -> request.clientId.value == userId.value
    }
}
