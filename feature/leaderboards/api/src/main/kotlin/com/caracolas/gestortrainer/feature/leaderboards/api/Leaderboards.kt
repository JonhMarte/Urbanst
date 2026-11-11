package com.caracolas.gestortrainer.feature.leaderboards.api

import com.caracolas.gestortrainer.core.model.PageRequest
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.flow.Flow

enum class BoardType { MY_CLIENTS, GLOBAL_USERS, GLOBAL_TRAINERS }

sealed interface BoardScope {
    data object Global : BoardScope
    data class Country(val countryCode: String) : BoardScope
    data class Discipline(val name: String) : BoardScope
    data class MyTrainer(val trainerId: TrainerId) : BoardScope
}

/** Solo nombre o alias, foto y puntos: nunca datos corporales. */
data class LeaderboardEntry(
    val position: Int,
    val userId: UserId,
    val displayName: String,
    val photoUrl: String?,
    val score: Double,
    val isMe: Boolean,
)

data class LeaderboardPage(val entries: List<LeaderboardEntry>, val page: Int, val hasMore: Boolean)

data class Position(val position: Int, val score: Double)

interface LeaderboardService {
    fun observe(type: BoardType, scope: BoardScope, page: PageRequest): Flow<LeaderboardPage>
    suspend fun myPosition(type: BoardType, userId: UserId): Result<Position?>
}
