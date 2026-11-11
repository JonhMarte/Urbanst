package com.caracolas.gestortrainer.feature.leaderboards.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.core.model.PageRequest
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardScope
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardType
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardEntry
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardPage
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardService
import com.caracolas.gestortrainer.feature.leaderboards.api.Position
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import com.caracolas.gestortrainer.feature.scoring.api.ScoreRepository
import com.caracolas.gestortrainer.feature.scoring.api.TrainerRankingPolicy
import com.caracolas.gestortrainer.feature.scoring.api.TrainerStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.time.LocalDate
import javax.inject.Inject

private data class RawEntry(val userId: UserId, val displayName: String, val photoUrl: String?, val score: Double)

/**
 * Las tres tablas (mis alumnos, alumnos globales, trainers globales) en memoria mientras no exista
 * el backend (Fase 2). La oficial la calculará el servicio Leaderboards del backend con los mismos datos.
 */
class DefaultLeaderboardService @Inject constructor(
    private val scoreRepository: ScoreRepository,
    private val clientRosterRepository: ClientRosterRepository,
    private val profileRepository: ProfileRepository,
    private val assignmentService: AssignmentService,
    private val reviewService: ReviewService,
    private val linkService: LinkService,
    private val trainerRankingPolicy: TrainerRankingPolicy,
) : LeaderboardService {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(type: BoardType, scope: BoardScope, page: PageRequest): Flow<LeaderboardPage> =
        rawEntries(type, scope).map { buildPage(it, page) }

    override suspend fun myPosition(type: BoardType, userId: UserId): Result<Position?> {
        val scope = when (type) {
            BoardType.MY_CLIENTS -> {
                val trainerId = linkService.observeLinkedTrainers(ClientId(userId.value)).first().firstOrNull()
                    ?: return Result.success(null)
                BoardScope.MyTrainer(trainerId)
            }
            BoardType.GLOBAL_USERS, BoardType.GLOBAL_TRAINERS -> BoardScope.Global
        }
        val entries = rawEntries(type, scope).first().sortedByDescending { it.score }
        val index = entries.indexOfFirst { it.userId == userId }
        if (index < 0) return Result.success(null)
        return Result.success(Position(index + 1, entries[index].score))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun rawEntries(type: BoardType, scope: BoardScope): Flow<List<RawEntry>> = when (type) {
        BoardType.MY_CLIENTS -> {
            val trainerId = (scope as? BoardScope.MyTrainer)?.trainerId
                ?: return flowOf(emptyList())
            clientRosterRepository.observeActiveClients(trainerId).flatMapLatest { cards ->
                if (cards.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(cards.map { card -> pointsEntry(UserId(card.clientId.value), card.displayName, card.photoUrl) }) { it.toList() }
                }
            }
        }
        BoardType.GLOBAL_USERS -> profileRepository.observeAllClients().flatMapLatest { clients ->
            val filtered = when (scope) {
                is BoardScope.Country -> clients.filter { it.personal.region.countryCode == scope.countryCode }
                else -> clients
            }
            if (filtered.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(filtered.map { c -> pointsEntry(c.userId, c.personal.fullName, c.photoUrl) }) { it.toList() }
            }
        }
        BoardType.GLOBAL_TRAINERS -> profileRepository.observeAllTrainers().flatMapLatest { trainers ->
            val filtered = when (scope) {
                is BoardScope.Country -> trainers.filter { it.personal.region.countryCode == scope.countryCode }
                is BoardScope.Discipline -> trainers.filter { it.mainDiscipline.equals(scope.name, ignoreCase = true) }
                else -> trainers
            }
            if (filtered.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(filtered.map { t -> trainerEntry(t.userId, t.personal.fullName, t.photoUrl) }) { it.toList() }
            }
        }
    }

    private fun pointsEntry(userId: UserId, displayName: String, photoUrl: String?): Flow<RawEntry> =
        scoreRepository.observeTotal(userId).map { RawEntry(userId, displayName, photoUrl, it.value.toDouble()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun trainerEntry(userId: UserId, displayName: String, photoUrl: String?): Flow<RawEntry> {
        val trainerId = TrainerId(userId.value)
        return combine(
            reviewService.observe(trainerId),
            clientRosterRepository.observeActiveClients(trainerId),
        ) { summary, roster -> summary to roster }
            .mapLatest { (summary, roster) ->
                val today = LocalDate.now()
                val window = DateRange(today.minusDays(29), today)
                val completionRates = roster.map { card ->
                    val assignments = assignmentService.observeCalendar(card.clientId, window).first()
                    if (assignments.isEmpty()) null else assignments.count { it.status == AssignmentStatus.DONE }.toDouble() / assignments.size
                }
                val completionRate = completionRates.filterNotNull().let { rates -> if (rates.isEmpty()) 0.0 else rates.average() }
                val retained = roster.count { card ->
                    assignmentService.observeCalendar(card.clientId, window).first().any { it.status == AssignmentStatus.DONE }
                }
                val retention = if (roster.isEmpty()) 0.0 else retained.toDouble() / roster.size
                val stats = TrainerStats(summary.averageStars, summary.count, completionRate, retention)
                RawEntry(userId, displayName, photoUrl, trainerRankingPolicy.score(stats))
            }
    }

    private fun buildPage(raw: List<RawEntry>, page: PageRequest): LeaderboardPage {
        val sorted = raw.sortedByDescending { it.score }
        val start = page.page * page.size
        val items = sorted.drop(start).take(page.size)
        val entries = items.mapIndexed { index, entry ->
            LeaderboardEntry(start + index + 1, entry.userId, entry.displayName, entry.photoUrl, entry.score, isMe = false)
        }
        return LeaderboardPage(entries, page.page, hasMore = start + items.size < sorted.size)
    }
}
