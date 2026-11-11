package com.caracolas.gestortrainer.feature.leaderboards.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.core.model.PageRequest
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import com.caracolas.gestortrainer.feature.clients.api.ClientCard
import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.core.model.LinkRequestId
import com.caracolas.gestortrainer.feature.clients.api.LinkRequest
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardScope
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardType
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import com.caracolas.gestortrainer.feature.reviews.api.ReviewSummary
import com.caracolas.gestortrainer.feature.scoring.api.Points
import com.caracolas.gestortrainer.feature.scoring.api.ScoreEntry
import com.caracolas.gestortrainer.feature.scoring.api.ScoreRepository
import com.caracolas.gestortrainer.feature.scoring.api.TrainerRankingPolicy
import com.caracolas.gestortrainer.feature.scoring.api.TrainerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

private class FakeClientRosterRepository(private val byTrainer: Map<TrainerId, List<ClientCard>>) : ClientRosterRepository {
    override fun observeActiveClients(trainerId: TrainerId) = MutableStateFlow(byTrainer[trainerId].orEmpty())
}

private class FakeProfileRepository(
    private val trainers: List<TrainerProfile> = emptyList(),
    private val clients: List<ClientProfile> = emptyList(),
) : ProfileRepository {
    override fun observeTrainer(id: TrainerId) = MutableStateFlow(trainers.find { it.userId.value == id.value })
    override fun observeClient(id: ClientId) = MutableStateFlow(clients.find { it.userId.value == id.value })
    override suspend fun saveTrainer(profile: TrainerProfile) = Result.success(Unit)
    override suspend fun saveClient(profile: ClientProfile) = Result.success(Unit)
    override suspend fun findTrainerByPublicId(publicId: PublicId) = trainers.find { it.publicId == publicId }
    override suspend fun findClientByPublicId(publicId: PublicId) = clients.find { it.publicId == publicId }
    override fun observeAllTrainers() = MutableStateFlow(trainers)
    override fun observeAllClients() = MutableStateFlow(clients)
}

private class FakeAssignmentService : AssignmentService {
    override suspend fun assign(activityId: com.caracolas.gestortrainer.core.model.ActivityId, clientId: ClientId, date: java.time.LocalDate) =
        Result.failure<com.caracolas.gestortrainer.core.model.AssignmentId>(UnsupportedOperationException())
    override suspend fun unassign(id: com.caracolas.gestortrainer.core.model.AssignmentId) = Result.success(Unit)
    override suspend fun setStatus(id: com.caracolas.gestortrainer.core.model.AssignmentId, status: AssignmentStatus) = Result.success(Unit)
    override suspend fun get(id: com.caracolas.gestortrainer.core.model.AssignmentId) =
        Result.failure<com.caracolas.gestortrainer.feature.calendar.api.Assignment>(NoSuchElementException())
    override fun observeCalendar(clientId: ClientId, range: DateRange) =
        MutableStateFlow(emptyList<com.caracolas.gestortrainer.feature.calendar.api.Assignment>())
}

private class FakeReviewService(private val summaries: Map<TrainerId, ReviewSummary> = emptyMap()) : ReviewService {
    override suspend fun rate(clientId: ClientId, trainerId: TrainerId, stars: Int, comment: String?) = Result.success(Unit)
    override fun observe(trainerId: TrainerId): Flow<ReviewSummary> =
        MutableStateFlow(summaries[trainerId] ?: ReviewSummary(trainerId, 0.0, 0, emptyList()))
}

private class FakeLinkService(private val linked: Map<ClientId, List<TrainerId>> = emptyMap()) : LinkService {
    override suspend fun requestByTrainerId(clientId: ClientId, trainerPublicId: PublicId) =
        Result.failure<LinkRequestId>(UnsupportedOperationException())
    override suspend fun addByClientId(trainerId: TrainerId, clientPublicId: PublicId) =
        Result.failure<LinkRequestId>(UnsupportedOperationException())
    override fun observeIncoming(userId: com.caracolas.gestortrainer.core.model.UserId) = MutableStateFlow(emptyList<LinkRequest>())
    override suspend fun accept(requestId: LinkRequestId) = Result.success(Unit)
    override suspend fun reject(requestId: LinkRequestId) = Result.success(Unit)
    override fun observeLinkedTrainers(clientId: ClientId) = MutableStateFlow(linked[clientId].orEmpty())
}

private class FakeTrainerRankingPolicy : TrainerRankingPolicy {
    override fun score(stats: TrainerStats): Double = stats.averageStars
}

private class FakeScoreRepository : ScoreRepository {
    private val totals = MutableStateFlow<Map<UserId, Points>>(emptyMap())

    override suspend fun record(entries: List<ScoreEntry>) {
        entries.forEach { entry -> totals.value = totals.value + (entry.userId to (totals.value[entry.userId] ?: Points.ZERO) + entry.points) }
    }

    override fun observeTotal(userId: UserId): Flow<Points> = totals.map { it[userId] ?: Points.ZERO }
    override suspend fun totalToday(userId: UserId, now: java.time.Instant) = totals.value[userId] ?: Points.ZERO
}

private fun card(id: String, points: Int) = ClientCard(ClientId(id), "Cliente $id", null, "—", points)

private fun trainer(id: String, country: String = "ES") = TrainerProfile(
    userId = UserId(id),
    publicId = PublicId("TR-$id"),
    personal = PersonalData("Trainer $id", "Española", Region(country), age = 30),
    mainDiscipline = "Gym",
    secondaryDisciplines = emptyList(),
    yearsAsTrainer = 3,
    bio = "",
    photoUrl = null,
)

private fun client(id: String, country: String = "ES") = ClientProfile(
    userId = UserId(id),
    publicId = PublicId("AL-$id"),
    personal = PersonalData("Cliente $id", "Española", Region(country), age = 25),
    body = null,
    photoUrl = null,
)

class DefaultLeaderboardServiceTest {
    private val trainerId = TrainerId("t1")

    private fun service(
        roster: Map<TrainerId, List<ClientCard>> = emptyMap(),
        trainers: List<TrainerProfile> = emptyList(),
        clients: List<ClientProfile> = emptyList(),
        reviews: Map<TrainerId, ReviewSummary> = emptyMap(),
        linked: Map<ClientId, List<TrainerId>> = emptyMap(),
        scores: Map<UserId, Int> = emptyMap(),
    ): DefaultLeaderboardService {
        val scoreRepository = FakeScoreRepository()
        return DefaultLeaderboardService(
            scoreRepository = scoreRepository,
            clientRosterRepository = FakeClientRosterRepository(roster),
            profileRepository = FakeProfileRepository(trainers, clients),
            assignmentService = FakeAssignmentService(),
            reviewService = FakeReviewService(reviews),
            linkService = FakeLinkService(linked),
            trainerRankingPolicy = FakeTrainerRankingPolicy(),
        ).also {
            kotlinx.coroutines.runBlocking {
                scores.forEach { (userId, points) ->
                    scoreRepository.record(listOf(ScoreEntry(userId, Points(points), "r", Instant.now())))
                }
            }
        }
    }

    @Test
    fun misAlumnosSeOrdenanPorPuntosDescendente() = runTest {
        val service = service(
            roster = mapOf(trainerId to listOf(card("c1", 0), card("c2", 0))),
            scores = mapOf(UserId("c1") to 10, UserId("c2") to 40),
        )

        val page = service.observe(BoardType.MY_CLIENTS, BoardScope.MyTrainer(trainerId), PageRequest()).first()

        assertEquals(listOf(UserId("c2"), UserId("c1")), page.entries.map { it.userId })
        assertEquals(listOf(1, 2), page.entries.map { it.position })
    }

    @Test
    fun laPaginacionRespetaElTamanoDePagina() = runTest {
        val service = service(
            roster = mapOf(trainerId to listOf(card("c1", 1), card("c2", 2), card("c3", 3))),
        )

        val page = service.observe(BoardType.MY_CLIENTS, BoardScope.MyTrainer(trainerId), PageRequest(page = 0, size = 2)).first()

        assertEquals(2, page.entries.size)
        assertTrue(page.hasMore)
    }

    @Test
    fun alumnosGlobalesFiltraPorPais() = runTest {
        val service = service(
            clients = listOf(client("c1", "ES"), client("c2", "FR")),
        )

        val page = service.observe(BoardType.GLOBAL_USERS, BoardScope.Country("ES"), PageRequest()).first()

        assertEquals(listOf(UserId("c1")), page.entries.map { it.userId })
    }

    @Test
    fun trainersGlobalesUsaLaPoliticaDeRanking() = runTest {
        val service = service(
            trainers = listOf(trainer("t1"), trainer("t2")),
            reviews = mapOf(TrainerId("t1") to ReviewSummary(TrainerId("t1"), 4.0, 5, emptyList())),
        )

        val page = service.observe(BoardType.GLOBAL_TRAINERS, BoardScope.Global, PageRequest()).first()

        assertEquals(UserId("t1"), page.entries.first().userId)
    }

    @Test
    fun miPosicionEnMisAlumnosResuelveElTrainerDelAlumno() = runTest {
        val service = service(
            roster = mapOf(trainerId to listOf(card("c1", 5), card("c2", 50))),
            linked = mapOf(ClientId("c1") to listOf(trainerId)),
            scores = mapOf(UserId("c1") to 5, UserId("c2") to 50),
        )

        val position = service.myPosition(BoardType.MY_CLIENTS, UserId("c1")).getOrThrow()

        assertEquals(2, position?.position)
    }

    @Test
    fun miPosicionSinTrainerVinculadoEsNula() = runTest {
        val service = service()

        val position = service.myPosition(BoardType.MY_CLIENTS, UserId("sin-trainer")).getOrThrow()

        assertNull(position)
    }
}
