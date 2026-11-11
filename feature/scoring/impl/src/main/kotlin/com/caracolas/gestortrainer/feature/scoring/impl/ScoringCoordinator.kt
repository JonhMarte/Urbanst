package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.core.events.ActivitySessionCompleted
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.events.ScoreAwarded
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import com.caracolas.gestortrainer.feature.scoring.api.ScoreRepository
import com.caracolas.gestortrainer.feature.scoring.api.ScoringContext
import com.caracolas.gestortrainer.feature.scoring.api.ScoringEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Escucha ActivitySessionCompleted, calcula el contexto de puntuación a partir del calendario
 * y la biblioteca del trainer, y publica ScoreAwarded. Puente entre tracking y scoring mientras
 * no exista el backend: el motor y las reglas (ScoringEngine/ScoringRule) son las mismas que usará él.
 */
@Singleton
class ScoringCoordinator @Inject constructor(
    private val eventBus: EventBus,
    private val scoringEngine: ScoringEngine,
    private val scoreRepository: ScoreRepository,
    private val assignmentService: AssignmentService,
    private val activityRepository: ActivityRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            eventBus.on<ActivitySessionCompleted>().collect { handle(it) }
        }
    }

    internal suspend fun handle(event: ActivitySessionCompleted) {
        val assignment = assignmentService.get(event.assignmentId).getOrNull() ?: return
        val activity = activityRepository.get(assignment.activityId).getOrNull()
        val userId = UserId(event.clientId.value)

        val dayAssignments = assignmentService
            .observeCalendar(event.clientId, DateRange(assignment.date, assignment.date))
            .first()
        val completedAllForDay = dayAssignments.isNotEmpty() && dayAssignments.all { it.status == AssignmentStatus.DONE }

        val context = ScoringContext(
            userId = userId,
            averageDuration = activity?.averageDuration,
            completedAllForDay = completedAllForDay,
            weeklyStreak = if (completedAllForDay) weeklyStreak(event.clientId, assignment.date) else 0,
            pointsToday = scoreRepository.totalToday(userId, event.occurredAt),
        )

        val awarded = scoringEngine.score(event, context)
        scoreRepository.record(awarded)
        awarded.forEach { entry ->
            eventBus.publish(ScoreAwarded(entry.userId, entry.points.value, entry.ruleId, entry.at))
        }
    }

    /** Semanas completas seguidas hasta [date] (inclusive), contando solo semanas con al menos una actividad. */
    private suspend fun weeklyStreak(clientId: ClientId, date: LocalDate): Int {
        var streak = 0
        var weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        repeat(MAX_WEEKS_LOOKBACK) {
            val week = assignmentService.observeCalendar(clientId, DateRange(weekStart, weekStart.plusDays(6))).first()
            val completed = week.isNotEmpty() && week.all { it.status == AssignmentStatus.DONE }
            if (!completed) return streak
            streak++
            weekStart = weekStart.minusWeeks(1)
        }
        return streak
    }

    companion object {
        private const val MAX_WEEKS_LOOKBACK = 104
    }
}
