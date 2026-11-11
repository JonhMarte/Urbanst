package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.scoring.api.Points
import com.caracolas.gestortrainer.feature.scoring.api.ScoreEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class InMemoryScoreRepositoryTest {
    private val repository = InMemoryScoreRepository()
    private val today = Instant.parse("2026-11-12T10:00:00Z")

    @Test
    fun sumaLosPuntosDeUnMismoUsuario() = runTest {
        repository.record(listOf(ScoreEntry(UserId("u1"), Points(10), "r1", today)))
        repository.record(listOf(ScoreEntry(UserId("u1"), Points(5), "r2", today)))

        assertEquals(Points(15), repository.observeTotal(UserId("u1")).first())
    }

    @Test
    fun noMezclaPuntosDeOtrosUsuarios() = runTest {
        repository.record(listOf(ScoreEntry(UserId("u1"), Points(10), "r1", today)))
        repository.record(listOf(ScoreEntry(UserId("u2"), Points(99), "r1", today)))

        assertEquals(Points(10), repository.observeTotal(UserId("u1")).first())
    }

    @Test
    fun totalTodaySoloCuentaElMismoDia() = runTest {
        val yesterday = today.minusSeconds(24 * 60 * 60)
        repository.record(listOf(ScoreEntry(UserId("u1"), Points(10), "r1", yesterday)))
        repository.record(listOf(ScoreEntry(UserId("u1"), Points(7), "r2", today)))

        assertEquals(Points(7), repository.totalToday(UserId("u1"), today))
    }
}
