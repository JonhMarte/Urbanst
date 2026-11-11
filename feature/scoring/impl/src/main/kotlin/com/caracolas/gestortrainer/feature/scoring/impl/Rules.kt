package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.core.events.ActivitySessionCompleted
import com.caracolas.gestortrainer.core.events.DomainEvent
import com.caracolas.gestortrainer.feature.scoring.api.Points
import com.caracolas.gestortrainer.feature.scoring.api.ScoringContext
import com.caracolas.gestortrainer.feature.scoring.api.ScoringRule
import java.time.Duration as JavaDuration
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.toKotlinDuration

/** Puntos si la duración real queda dentro de un margen del tiempo promedio del trainer. */
class TimeWindowRule @Inject constructor() : ScoringRule {
    override val id = "time_window"

    override fun evaluate(event: DomainEvent, context: ScoringContext): Points {
        if (event !is ActivitySessionCompleted) return Points.ZERO
        val expected = context.averageDuration ?: return Points.ZERO
        if (expected.inWholeSeconds <= 0) return Points.ZERO
        val actual = JavaDuration.between(event.startedAt, event.finishedAt).toKotlinDuration()
        val deviation = abs(actual.inWholeSeconds - expected.inWholeSeconds).toDouble() / expected.inWholeSeconds
        return if (deviation <= TOLERANCE) Points(POINTS) else Points.ZERO
    }

    companion object {
        const val TOLERANCE = 0.25 // ±25 % del tiempo promedio
        const val POINTS = 10
    }
}

/** Puntos por subir una foto de la actividad. */
class PhotoEvidenceRule @Inject constructor() : ScoringRule {
    override val id = "photo_evidence"
    override fun evaluate(event: DomainEvent, context: ScoringContext): Points =
        if (event is ActivitySessionCompleted && event.hasPhoto) Points(5) else Points.ZERO
}

/** Bonus por completar todas las actividades del día asignado. */
class DailyCompletionRule @Inject constructor() : ScoringRule {
    override val id = "daily_completion"
    override fun evaluate(event: DomainEvent, context: ScoringContext): Points =
        if (event is ActivitySessionCompleted && context.completedAllForDay) Points(15) else Points.ZERO
}

/** Bonus por constancia: semanas completas seguidas (máx. 4 semanas acumuladas). */
class WeeklyStreakRule @Inject constructor() : ScoringRule {
    override val id = "weekly_streak"
    override fun evaluate(event: DomainEvent, context: ScoringContext): Points =
        if (event is ActivitySessionCompleted && context.completedAllForDay && context.weeklyStreak > 1) {
            Points(minOf(context.weeklyStreak, 4) * 2)
        } else {
            Points.ZERO
        }
}
