package com.caracolas.gestortrainer.feature.clients.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.LinkRequestId
import com.caracolas.gestortrainer.core.model.NoteId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

enum class LinkDirection { CLIENT_TO_TRAINER, TRAINER_TO_CLIENT }
enum class LinkStatus { PENDING, ACCEPTED, REJECTED }

data class LinkRequest(
    val id: LinkRequestId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val direction: LinkDirection,
    val status: LinkStatus,
    val createdAt: Instant,
)

/** Tarjeta de un alumno activo en la lista del trainer. */
data class ClientCard(
    val clientId: ClientId,
    val displayName: String,
    val photoUrl: String?,
    val todaySummary: String,
    val points: Int,
)

data class Note(
    val id: NoteId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val text: String,
    val createdAt: Instant,
)

enum class LinkError { TRAINER_NOT_FOUND, CLIENT_NOT_FOUND, ALREADY_LINKED, REQUEST_ALREADY_PENDING }

class LinkException(val error: LinkError) : Exception()

interface LinkService {
    /** El alumno pide unirse a un trainer por su ID. */
    suspend fun requestByTrainerId(clientId: ClientId, trainerPublicId: PublicId): Result<LinkRequestId>
    /** El trainer agrega a un alumno por su ID (ADD New User). */
    suspend fun addByClientId(trainerId: TrainerId, clientPublicId: PublicId): Result<LinkRequestId>
    fun observeIncoming(userId: UserId): Flow<List<LinkRequest>>
    suspend fun accept(requestId: LinkRequestId): Result<Unit>
    suspend fun reject(requestId: LinkRequestId): Result<Unit>
    /** Trainers ya vinculados a este alumno (para valorarlos). */
    fun observeLinkedTrainers(clientId: ClientId): Flow<List<TrainerId>>
}

interface ClientRosterRepository {
    fun observeActiveClients(trainerId: TrainerId): Flow<List<ClientCard>>
}

interface TrainerNotesService {
    suspend fun addNote(trainerId: TrainerId, clientId: ClientId, text: String): Result<NoteId>
    fun observeNotes(clientId: ClientId): Flow<List<Note>>
}
