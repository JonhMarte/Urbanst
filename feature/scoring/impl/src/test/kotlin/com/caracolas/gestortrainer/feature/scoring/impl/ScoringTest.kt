package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.core.events.ActivitySessionCompleted
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.SessionId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.scoring.api.Points
import com.caracolas.gestortrainer.feature.scoring.api.ScoringContext
import com.caracolas.gestortrainer.feature.scoring.api.TrainerStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class ScoringTest {
    private val start = Instant.parse("2026-11-12T10:00:00Z")

    private fun session(minutes: Long, photo: Boolean) = ActivitySessionCompleted(
        sessionId = SessionId("s1"),
        assignmentId = AssignmentId("a1"),
        clientId = ClientId("c1"),
        startedAt = start,
        finishedAt = start.plusSeconds(minutes * 60),
        hasPhoto = photo,
        occurredAt = start.plusSeconds(minutes * 60),
    )

    private fun context(pointsToday: Int = 0, allDone: Boolean = false, streak: Int = 0) = ScoringContext(
        userId = UserId("u1"),
        averageDuration = 45.minutes,
        completedAllForDay = allDone,
        weeklyStreak = streak,
        pointsToday = Points(pointsToday),
    )

    private val engine = DefaultScoringEngine(
        setOf(TimeWindowRule(), PhotoEvidenceRule(), DailyCompletionRule(), WeeklyStreakRule()),
    )

    @Test
    fun duracionDentroDelMargenPuntua() {
        assertEquals(Points(10), TimeWindowRule().evaluate(session(50, false), context()))
    }

    @Test
    fun duracionMuyCortaNoPuntua() {
        assertEquals(Points.ZERO, TimeWindowRule().evaluate(session(5, false), context()))
    }

    @Test
    fun elLimiteDiarioSeRespeta() {
        val total = engine.score(session(45, true), context(pointsToday = 55, allDone = true)).sumOf { it.points.value }
        assertEquals(5, total)
    }

    @Test
    fun pocasValoracionesPesanMenosQueMuchas() {
        val policy = BayesianTrainerRankingPolicy()
        val one = policy.score(TrainerStats(5.0, 1, 0.8, 0.8))
        val many = policy.score(TrainerStats(4.8, 50, 0.8, 0.8))
        assertTrue(many > one)
    }
}
