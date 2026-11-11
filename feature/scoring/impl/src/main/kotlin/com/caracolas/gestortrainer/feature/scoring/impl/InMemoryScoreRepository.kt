package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.scoring.api.Points
import com.caracolas.gestortrainer.feature.scoring.api.ScoreEntry
import com.caracolas.gestortrainer.feature.scoring.api.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/** Puntos acumulados en memoria mientras no exista el backend (Fase 2). */
@Singleton
class InMemoryScoreRepository @Inject constructor() : ScoreRepository {
    private val entries = MutableStateFlow<List<ScoreEntry>>(emptyList())

    override suspend fun record(entries: List<ScoreEntry>) {
        if (entries.isEmpty()) return
        this.entries.value = this.entries.value + entries
    }

    override fun observeTotal(userId: UserId): Flow<Points> = entries.map { list ->
        list.filter { it.userId == userId }.fold(Points.ZERO) { acc, entry -> acc + entry.points }
    }

    override suspend fun totalToday(userId: UserId, now: Instant): Points {
        val today = now.atZone(ZoneOffset.UTC).toLocalDate()
        return entries.value
            .filter { it.userId == userId && it.at.atZone(ZoneOffset.UTC).toLocalDate() == today }
            .fold(Points.ZERO) { acc, entry -> acc + entry.points }
    }
}
