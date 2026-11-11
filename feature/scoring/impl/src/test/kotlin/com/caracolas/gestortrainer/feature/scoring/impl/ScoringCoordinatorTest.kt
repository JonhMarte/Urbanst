package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.core.events.ActivitySessionCompleted
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.ScoreAwarded
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.core.model.ExerciseMedia
import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.core.model.SessionId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.activities.api.Activity
import com.caracolas.gestortrainer.feature.activities.api.ActivityDraft
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.activities.api.ActivityType
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

private class FakeAssignmentService : AssignmentService {
    val all = MutableStateFlow<Map<AssignmentId, Assignment>>(emptyMap())

    fun seed(id: String, clientId: ClientId, date: LocalDate, status: AssignmentStatus) {
        val assignmentId = AssignmentId(id)
        all.value = all.value + (assignmentId to Assignment(assignmentId, ActivityId("a1"), clientId, date, status))
    }

    override suspend fun assign(activityId: ActivityId, clientId: ClientId, date: LocalDate) = Result.success(AssignmentId("x"))
    override suspend fun unassign(id: AssignmentId) = Result.success(Unit)
    override suspend fun setStatus(id: AssignmentId, status: AssignmentStatus) = Result.success(Unit)
    override suspend fun get(id: AssignmentId): Result<Assignment> =
        all.value[id]?.let { Result.success(it) } ?: Result.failure(NoSuchElementException())

    override fun observeCalendar(clientId: ClientId, range: DateRange) = all.map { map ->
        map.values.filter {
            it.clientId == clientId && !it.date.isBefore(range.start) && !it.date.isAfter(range.endInclusive)
        }
    }
}

private class FakeActivityRepository(private val averageDuration: kotlin.time.Duration) : ActivityRepository {
    override fun observeLibrary(trainerId: TrainerId) = MutableStateFlow<List<Activity>>(emptyList())
    override suspend fun get(id: ActivityId) = Result.success(
        Activity(id, TrainerId("t1"), ActivityType.PHYSICAL, "T", "D", averageDuration, listOf(MuscleId("core")), emptyList<ExerciseMedia>()),
    )
    override suspend fun save(draft: ActivityDraft) = Result.success(ActivityId("a1"))
    override suspend fun delete(id: ActivityId) = Result.success(Unit)
}

class ScoringCoordinatorTest {
    private val monday = LocalDate.of(2026, 11, 9)
    private val eventBus = InMemoryEventBus()
    private val assignmentService = FakeAssignmentService()
    private val activityRepository = FakeActivityRepository(45.minutes)
    private val scoreRepository = InMemoryScoreRepository()
    private val engine = DefaultScoringEngine(
        setOf(TimeWindowRule(), PhotoEvidenceRule(), DailyCompletionRule(), WeeklyStreakRule()),
    )
    private val coordinator = ScoringCoordinator(eventBus, engine, scoreRepository, assignmentService, activityRepository)

    private fun completedEvent(assignmentId: String, date: LocalDate, hasPhoto: Boolean) = ActivitySessionCompleted(
        sessionId = SessionId("s-$assignmentId"),
        assignmentId = AssignmentId(assignmentId),
        clientId = ClientId("c1"),
        startedAt = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
        finishedAt = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).plusSeconds(45 * 60),
        hasPhoto = hasPhoto,
        occurredAt = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
    )

    @Test
    fun unicaActividadDelDiaCompletadaConFotoOtorgaPuntosYPublicaScoreAwarded() = runTest(UnconfinedTestDispatcher()) {
        assignmentService.seed("a1", ClientId("c1"), monday, AssignmentStatus.DONE)
        val received = async { eventBus.on<ScoreAwarded>().first() }

        coordinator.handle(completedEvent("a1", monday, hasPhoto = true))

        val total = scoreRepository.observeTotal(UserId("c1")).first()
        // tiempo (10) + foto (5) + día completo, única actividad (15) = 30
        assertEquals(30, total.value)
        assertEquals(UserId("c1"), received.await().userId)
    }

    @Test
    fun diaConUnaActividadPendienteNoCuentaComoDiaCompleto() = runTest {
        assignmentService.seed("a1", ClientId("c1"), monday, AssignmentStatus.DONE)
        assignmentService.seed("a2", ClientId("c1"), monday, AssignmentStatus.PENDING)

        coordinator.handle(completedEvent("a1", monday, hasPhoto = false))

        // tiempo (10) + foto (0) + día completo (0, falta a2) = 10
        assertEquals(10, scoreRepository.observeTotal(UserId("c1")).first().value)
    }

    @Test
    fun dosSemanasCompletasSeguidasDanBonusDeConstancia() = runTest {
        val lastWeekMonday = monday.minusWeeks(1)
        assignmentService.seed("prev", ClientId("c1"), lastWeekMonday, AssignmentStatus.DONE)
        assignmentService.seed("a1", ClientId("c1"), monday, AssignmentStatus.DONE)

        coordinator.handle(completedEvent("a1", monday, hasPhoto = false))

        // tiempo (10) + día completo (15) + racha de 2 semanas (2*2=4) = 29
        assertEquals(29, scoreRepository.observeTotal(UserId("c1")).first().value)
    }

    @Test
    fun actividadDeUnaAsignacionQueYaNoExisteSeIgnoraSinFallar() = runTest {
        coordinator.handle(completedEvent("no-existe", monday, hasPhoto = true))

        assertTrue(scoreRepository.observeTotal(UserId("c1")).first().value == 0)
    }
}
