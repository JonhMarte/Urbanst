package com.caracolas.gestortrainer.feature.calendar.api

import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class AssignmentStatus { PENDING, IN_PROGRESS, DONE, MISSED }

data class Assignment(
    val id: AssignmentId,
    val activityId: ActivityId,
    val clientId: ClientId,
    val date: LocalDate,
    val status: AssignmentStatus,
)

interface AssignmentService {
    /** Arrastrar una actividad de la biblioteca a un día del alumno. */
    suspend fun assign(activityId: ActivityId, clientId: ClientId, date: LocalDate): Result<AssignmentId>
    suspend fun unassign(id: AssignmentId): Result<Unit>
    /** Activar (PENDING → IN_PROGRESS) y finalizar (IN_PROGRESS → DONE) una asignación. */
    suspend fun setStatus(id: AssignmentId, status: AssignmentStatus): Result<Unit>
    suspend fun get(id: AssignmentId): Result<Assignment>
    fun observeCalendar(clientId: ClientId, range: DateRange): Flow<List<Assignment>>
}
