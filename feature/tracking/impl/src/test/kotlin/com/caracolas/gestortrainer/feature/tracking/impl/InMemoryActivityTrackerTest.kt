package com.caracolas.gestortrainer.feature.tracking.impl

import com.caracolas.gestortrainer.core.events.ActivitySessionCompleted
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.core.model.MediaKind
import com.caracolas.gestortrainer.core.model.RemoteMedia
import com.caracolas.gestortrainer.core.model.SessionId
import com.caracolas.gestortrainer.feature.calendar.api.Assignment
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

private class FakeAssignmentService : AssignmentService {
    val all = MutableStateFlow<Map<AssignmentId, Assignment>>(emptyMap())

    fun seed(clientId: ClientId, status: AssignmentStatus = AssignmentStatus.PENDING): AssignmentId {
        val id = AssignmentId(UUID.randomUUID().toString())
        all.value = all.value + (id to Assignment(id, ActivityId("a1"), clientId, LocalDate.now(), status))
        return id
    }

    override suspend fun assign(activityId: ActivityId, clientId: ClientId, date: LocalDate) =
        Result.success(AssignmentId("x"))

    override suspend fun unassign(id: AssignmentId): Result<Unit> = Result.success(Unit)

    override suspend fun setStatus(id: AssignmentId, status: AssignmentStatus): Result<Unit> {
        val current = all.value[id] ?: return Result.failure(NoSuchElementException())
        all.value = all.value + (id to current.copy(status = status))
        return Result.success(Unit)
    }

    override suspend fun get(id: AssignmentId): Result<Assignment> =
        all.value[id]?.let { Result.success(it) } ?: Result.failure(NoSuchElementException())

    override fun observeCalendar(clientId: ClientId, range: DateRange) = all.map { it.values.toList() }
}

class InMemoryActivityTrackerTest {
    private val eventBus = InMemoryEventBus()
    private val assignmentService = FakeAssignmentService()
    private val tracker = InMemoryActivityTracker(assignmentService, eventBus)

    @Test
    fun activarPoneLaAsignacionEnCursoYRecuerdaLaSesion() = runTest {
        val assignmentId = assignmentService.seed(ClientId("c1"))

        val sessionId = tracker.start(assignmentId).getOrThrow()

        assertEquals(AssignmentStatus.IN_PROGRESS, assignmentService.get(assignmentId).getOrThrow().status)
        assertEquals(sessionId, tracker.activeSessionFor(assignmentId))
    }

    @Test
    fun finalizarSinFotoCompletaConHasPhotoFalso() = runTest(UnconfinedTestDispatcher()) {
        val assignmentId = assignmentService.seed(ClientId("c1"))
        val sessionId = tracker.start(assignmentId).getOrThrow()
        val received = async { eventBus.on<ActivitySessionCompleted>().first() }

        val summary = tracker.finish(sessionId).getOrThrow()

        assertFalse(summary.hasPhoto)
        assertFalse(received.await().hasPhoto)
        assertEquals(AssignmentStatus.DONE, assignmentService.get(assignmentId).getOrThrow().status)
        assertNull(tracker.activeSessionFor(assignmentId))
    }

    @Test
    fun adjuntarFotoAntesDeFinalizarMarcaHasPhotoVerdadero() = runTest {
        val assignmentId = assignmentService.seed(ClientId("c1"))
        val sessionId = tracker.start(assignmentId).getOrThrow()

        tracker.attachPhoto(sessionId, RemoteMedia("m1", "url", MediaKind.ACTIVITY_PHOTO))
        val summary = tracker.finish(sessionId).getOrThrow()

        assertTrue(summary.hasPhoto)
    }

    @Test
    fun activarUnaAsignacionQueNoEstaPendienteFalla() = runTest {
        val assignmentId = assignmentService.seed(ClientId("c1"), AssignmentStatus.IN_PROGRESS)

        val result = tracker.start(assignmentId)

        assertTrue(result.isFailure)
    }

    @Test
    fun finalizarUnaSesionInexistenteFalla() = runTest {
        val result = tracker.finish(SessionId("no-existe"))
        assertTrue(result.isFailure)
    }
}
