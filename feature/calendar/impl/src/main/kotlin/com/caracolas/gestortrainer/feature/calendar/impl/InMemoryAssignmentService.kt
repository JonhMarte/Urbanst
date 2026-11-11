package com.caracolas.gestortrainer.feature.calendar.impl

import com.caracolas.gestortrainer.core.events.ActivityAssigned
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.feature.calendar.api.Assignment
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Calendario en memoria mientras no exista el backend AWS (Fase 1). */
@Singleton
class InMemoryAssignmentService @Inject constructor(
    private val eventBus: EventBus,
) : AssignmentService {
    private val assignments = MutableStateFlow<Map<AssignmentId, Assignment>>(emptyMap())

    override suspend fun assign(activityId: ActivityId, clientId: ClientId, date: LocalDate): Result<AssignmentId> {
        val id = AssignmentId(UUID.randomUUID().toString())
        val assignment = Assignment(id, activityId, clientId, date, AssignmentStatus.PENDING)
        assignments.value = assignments.value + (id to assignment)
        eventBus.publish(ActivityAssigned(id, activityId, clientId, date, Instant.now()))
        return Result.success(id)
    }

    override suspend fun unassign(id: AssignmentId): Result<Unit> {
        assignments.value = assignments.value - id
        return Result.success(Unit)
    }

    override suspend fun setStatus(id: AssignmentId, status: AssignmentStatus): Result<Unit> {
        val current = assignments.value[id]
            ?: return Result.failure(NoSuchElementException("No existe la asignación $id"))
        assignments.value = assignments.value + (id to current.copy(status = status))
        return Result.success(Unit)
    }

    override suspend fun get(id: AssignmentId): Result<Assignment> =
        assignments.value[id]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("No existe la asignación $id"))

    override fun observeCalendar(clientId: ClientId, range: DateRange): Flow<List<Assignment>> =
        assignments.map { map ->
            map.values.filter {
                it.clientId == clientId && !it.date.isBefore(range.start) && !it.date.isAfter(range.endInclusive)
            }
        }
}
