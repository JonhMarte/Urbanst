package com.caracolas.gestortrainer.feature.tracking.impl

import com.caracolas.gestortrainer.core.events.ActivitySessionCompleted
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.RemoteMedia
import com.caracolas.gestortrainer.core.model.SessionId
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import com.caracolas.gestortrainer.feature.tracking.api.ActivityTracker
import com.caracolas.gestortrainer.feature.tracking.api.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private data class TrackedSession(
    val id: SessionId,
    val assignmentId: AssignmentId,
    val startedAt: Instant,
    val photo: RemoteMedia? = null,
)

/** Registro de activación/finalización en memoria mientras no exista el backend (Fase 2). */
@Singleton
class InMemoryActivityTracker @Inject constructor(
    private val assignmentService: AssignmentService,
    private val eventBus: EventBus,
) : ActivityTracker {
    private val sessions = MutableStateFlow<Map<SessionId, TrackedSession>>(emptyMap())

    override suspend fun start(assignmentId: AssignmentId): Result<SessionId> {
        val assignment = assignmentService.get(assignmentId).getOrElse { return Result.failure(it) }
        if (assignment.status != AssignmentStatus.PENDING) {
            return Result.failure(IllegalStateException("La asignación ya está en curso o finalizada"))
        }
        val id = SessionId(UUID.randomUUID().toString())
        sessions.value = sessions.value + (id to TrackedSession(id, assignmentId, Instant.now()))
        assignmentService.setStatus(assignmentId, AssignmentStatus.IN_PROGRESS)
            .onFailure { sessions.value = sessions.value - id; return Result.failure(it) }
        return Result.success(id)
    }

    override suspend fun attachPhoto(sessionId: SessionId, photo: RemoteMedia): Result<Unit> {
        val session = sessions.value[sessionId]
            ?: return Result.failure(NoSuchElementException("No existe la sesión $sessionId"))
        sessions.value = sessions.value + (sessionId to session.copy(photo = photo))
        return Result.success(Unit)
    }

    override suspend fun finish(sessionId: SessionId): Result<SessionSummary> {
        val session = sessions.value[sessionId]
            ?: return Result.failure(NoSuchElementException("No existe la sesión $sessionId"))
        val assignment = assignmentService.get(session.assignmentId).getOrElse { return Result.failure(it) }
        val finishedAt = Instant.now()
        assignmentService.setStatus(session.assignmentId, AssignmentStatus.DONE)
            .onFailure { return Result.failure(it) }
        sessions.value = sessions.value - sessionId

        val hasPhoto = session.photo != null
        eventBus.publish(
            ActivitySessionCompleted(
                sessionId = sessionId,
                assignmentId = session.assignmentId,
                clientId = assignment.clientId,
                startedAt = session.startedAt,
                finishedAt = finishedAt,
                hasPhoto = hasPhoto,
                occurredAt = finishedAt,
            ),
        )
        return Result.success(
            SessionSummary(sessionId, session.assignmentId, session.startedAt, finishedAt, hasPhoto),
        )
    }

    override fun activeSessionFor(assignmentId: AssignmentId): SessionId? =
        sessions.value.values.find { it.assignmentId == assignmentId }?.id
}
