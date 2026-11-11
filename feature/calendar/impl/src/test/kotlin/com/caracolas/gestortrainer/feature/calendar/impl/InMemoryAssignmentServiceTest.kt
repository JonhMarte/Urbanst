package com.caracolas.gestortrainer.feature.calendar.impl

import com.caracolas.gestortrainer.core.events.ActivityAssigned
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InMemoryAssignmentServiceTest {
    private val eventBus = InMemoryEventBus()
    private val service = InMemoryAssignmentService(eventBus)
    private val monday = LocalDate.of(2025, 1, 6)
    private val week = DateRange(monday, monday.plusDays(6))

    @Test
    fun asignarUnaActividadLaDejaPendienteYPublicaElEvento() = runTest(UnconfinedTestDispatcher()) {
        val received = async { eventBus.on<ActivityAssigned>().first() }

        val id = service.assign(ActivityId("a1"), ClientId("c1"), monday).getOrThrow()

        val calendar = service.observeCalendar(ClientId("c1"), week).first()
        assertEquals(1, calendar.size)
        assertEquals(AssignmentStatus.PENDING, calendar.first().status)
        assertEquals(id, received.await().assignmentId)
    }

    @Test
    fun observeCalendarSoloDevuelveLoDeEseAlumnoYDentroDelRango() = runTest {
        service.assign(ActivityId("a1"), ClientId("c1"), monday)
        service.assign(ActivityId("a2"), ClientId("otro"), monday)
        service.assign(ActivityId("a3"), ClientId("c1"), monday.plusDays(10))

        val calendar = service.observeCalendar(ClientId("c1"), week).first()

        assertEquals(1, calendar.size)
        assertEquals(ActivityId("a1"), calendar.first().activityId)
    }

    @Test
    fun activarYFinalizarCambianElEstadoEnOrden() = runTest {
        val id = service.assign(ActivityId("a1"), ClientId("c1"), monday).getOrThrow()

        service.setStatus(id, AssignmentStatus.IN_PROGRESS)
        assertEquals(AssignmentStatus.IN_PROGRESS, service.observeCalendar(ClientId("c1"), week).first().first().status)

        service.setStatus(id, AssignmentStatus.DONE)
        assertEquals(AssignmentStatus.DONE, service.observeCalendar(ClientId("c1"), week).first().first().status)
    }

    @Test
    fun cambiarEstadoDeUnaAsignacionInexistenteFalla() = runTest {
        val result = service.setStatus(AssignmentId("no-existe"), AssignmentStatus.DONE)
        assertTrue(result.isFailure)
    }

    @Test
    fun quitarUnaAsignacionLaSacaDelCalendario() = runTest {
        val id = service.assign(ActivityId("a1"), ClientId("c1"), monday).getOrThrow()

        service.unassign(id)

        assertTrue(service.observeCalendar(ClientId("c1"), week).first().isEmpty())
    }
}
