package com.caracolas.gestortrainer.feature.plan.impl

import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.feature.calendar.api.Assignment
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeAssignmentService : AssignmentService {
    val all = MutableStateFlow<List<Assignment>>(emptyList())

    override suspend fun assign(activityId: ActivityId, clientId: ClientId, date: LocalDate): Result<AssignmentId> {
        val id = AssignmentId("${activityId.value}-${date}")
        all.value = all.value + Assignment(id, activityId, clientId, date, AssignmentStatus.PENDING)
        return Result.success(id)
    }

    override suspend fun unassign(id: AssignmentId): Result<Unit> {
        all.value = all.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun setStatus(id: AssignmentId, status: AssignmentStatus): Result<Unit> {
        all.value = all.value.map { if (it.id == id) it.copy(status = status) else it }
        return Result.success(Unit)
    }

    override suspend fun get(id: AssignmentId): Result<Assignment> =
        all.value.find { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException())

    override fun observeCalendar(clientId: ClientId, range: DateRange) = all.map { list ->
        list.filter {
            it.clientId == clientId && !it.date.isBefore(range.start) && !it.date.isAfter(range.endInclusive)
        }
    }
}

class DefaultWeeklyPlanServiceTest {
    private val assignmentService = FakeAssignmentService()
    private val service = DefaultWeeklyPlanService(assignmentService)
    private val monday = LocalDate.of(2025, 1, 6)

    @Test
    fun laSemanaAgrupaLasAsignacionesPorDia() = runBlocking {
        assignmentService.assign(ActivityId("a1"), ClientId("c1"), monday)
        assignmentService.assign(ActivityId("a2"), ClientId("c1"), monday.plusDays(2))

        val plan = service.observeWeek(ClientId("c1"), monday).first()

        assertEquals(7, plan.days.size)
        assertEquals(1, plan.days.getValue(monday).size)
        assertEquals(1, plan.days.getValue(monday.plusDays(2)).size)
        assertTrue(plan.days.getValue(monday.plusDays(1)).isEmpty())
    }

    @Test
    fun unAlumnoSoloVeSusPropiasAsignaciones() = runBlocking {
        assignmentService.assign(ActivityId("a1"), ClientId("c1"), monday)
        assignmentService.assign(ActivityId("a2"), ClientId("otro"), monday)

        val plan = service.observeWeek(ClientId("c1"), monday).first()

        assertEquals(1, plan.days.getValue(monday).size)
        assertEquals(ActivityId("a1"), plan.days.getValue(monday).first().activityId)
    }

    @Test
    fun unaAsignacionFueraDeLaSemanaNoApareceEnElPlan() = runBlocking {
        assignmentService.assign(ActivityId("a1"), ClientId("c1"), monday.plusDays(10))

        val plan = service.observeWeek(ClientId("c1"), monday).first()

        assertTrue(plan.days.values.all { it.isEmpty() })
    }
}
