package com.caracolas.gestortrainer.feature.tracking.api

import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.RemoteMedia
import com.caracolas.gestortrainer.core.model.SessionId
import java.time.Instant

data class SessionSummary(
    val sessionId: SessionId,
    val assignmentId: AssignmentId,
    val startedAt: Instant,
    val finishedAt: Instant,
    val hasPhoto: Boolean,
)

/** Activar y desactivar una actividad. Al terminar se publica ActivitySessionCompleted. */
interface ActivityTracker {
    suspend fun start(assignmentId: AssignmentId): Result<SessionId>
    suspend fun finish(sessionId: SessionId): Result<SessionSummary>
    suspend fun attachPhoto(sessionId: SessionId, photo: RemoteMedia): Result<Unit>
    /** La sesión en curso de esa asignación, si la hay (para recuperarla tras salir de la pantalla). */
    fun activeSessionFor(assignmentId: AssignmentId): SessionId?
}
