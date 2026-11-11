package com.caracolas.gestortrainer.feature.scoring.api

import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.core.events.DomainEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import kotlin.time.Duration

@JvmInline
value class Points(val value: Int) {
    operator fun plus(other: Points) = Points(value + other.value)
    companion object { val ZERO = Points(0) }
}

data class ScoreEntry(val userId: UserId, val points: Points, val ruleId: String, val at: Instant)

/** Lo que una regla necesita saber además del evento. */
data class ScoringContext(
    val userId: UserId,
    /** Tiempo promedio que fijó el trainer para la actividad (oculto al alumno). */
    val averageDuration: Duration?,
    /** ¿Completó hoy todas las actividades asignadas para hoy? */
    val completedAllForDay: Boolean,
    /** Semanas completas seguidas, contando la actual. */
    val weeklyStreak: Int,
    /** Puntos ya ganados hoy (para el límite diario). */
    val pointsToday: Points,
)

/** Una regla de puntuación. Se añaden o quitan sin tocar las demás. */
interface ScoringRule {
    val id: String
    fun evaluate(event: DomainEvent, context: ScoringContext): Points
}

/** Aplica todas las reglas y los límites. La puntuación oficial la calcula el servidor. */
interface ScoringEngine {
    fun score(event: DomainEvent, context: ScoringContext): List<ScoreEntry>
}

data class TrainerStats(
    val averageStars: Double,
    val reviewCount: Int,
    /** 0.0 – 1.0 */
    val clientCompletionRate: Double,
    /** 0.0 – 1.0, alumnos activos a 30 días */
    val retention30d: Double,
)

interface TrainerRankingPolicy {
    fun score(stats: TrainerStats): Double
}

/** Histórico de puntos otorgados. La oficial la lleva el backend; aquí vive mientras no exista. */
interface ScoreRepository {
    suspend fun record(entries: List<ScoreEntry>)
    fun observeTotal(userId: UserId): Flow<Points>
    suspend fun totalToday(userId: UserId, now: Instant = Instant.now()): Points
}
