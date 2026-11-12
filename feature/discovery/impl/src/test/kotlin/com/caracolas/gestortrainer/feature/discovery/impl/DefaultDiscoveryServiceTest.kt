package com.caracolas.gestortrainer.feature.discovery.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.discovery.api.TrainerQuery
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import com.caracolas.gestortrainer.feature.reviews.api.ReviewSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeProfileRepository(private val trainers: List<TrainerProfile>) : ProfileRepository {
    override fun observeTrainer(id: TrainerId) = MutableStateFlow(trainers.find { it.userId.value == id.value })
    override fun observeClient(id: ClientId) = MutableStateFlow<ClientProfile?>(null)
    override suspend fun saveTrainer(profile: TrainerProfile) = Result.success(Unit)
    override suspend fun saveClient(profile: ClientProfile) = Result.success(Unit)
    override suspend fun findTrainerByPublicId(publicId: PublicId) = trainers.find { it.publicId == publicId }
    override suspend fun findClientByPublicId(publicId: PublicId) = null
    override fun observeAllTrainers() = MutableStateFlow(trainers)
    override fun observeAllClients() = MutableStateFlow(emptyList<ClientProfile>())
}

private class FakeReviewService(private val stars: Map<String, Double> = emptyMap()) : ReviewService {
    override suspend fun rate(clientId: ClientId, trainerId: TrainerId, stars: Int, comment: String?) = Result.success(Unit)
    override fun observe(trainerId: TrainerId): Flow<ReviewSummary> =
        MutableStateFlow(ReviewSummary(trainerId, stars[trainerId.value] ?: 0.0, 0, emptyList()))
}

private fun trainer(id: String, discipline: String, name: String = "Trainer $id") = TrainerProfile(
    userId = UserId(id),
    publicId = PublicId("TR-$id"),
    personal = PersonalData(name, "Española", Region("ES"), age = 30),
    mainDiscipline = discipline,
    secondaryDisciplines = emptyList(),
    yearsAsTrainer = 2,
    bio = "",
    photoUrl = null,
)

class DefaultDiscoveryServiceTest {
    @Test
    fun buscarPorDisciplinaSoloDevuelveCoincidencias() = runTest {
        val service = DefaultDiscoveryService(
            FakeProfileRepository(listOf(trainer("1", "Yoga"), trainer("2", "Running"))),
            FakeReviewService(),
        )

        val results = service.searchTrainers(TrainerQuery(discipline = "Yoga")).first()

        assertEquals(1, results.size)
        assertEquals("Yoga", results.first().mainDiscipline)
    }

    @Test
    fun lasCategoriasPopularesCuentanTrainersPorDisciplina() = runTest {
        val service = DefaultDiscoveryService(
            FakeProfileRepository(listOf(trainer("1", "Yoga"), trainer("2", "Yoga"), trainer("3", "Running"))),
            FakeReviewService(),
        )

        val categories = service.popularCategories(limit = 6).first()

        val yoga = categories.first { it.discipline == "Yoga" }
        assertEquals(2, yoga.trainerCount)
        assertTrue(categories.indexOf(yoga) < categories.indexOfFirst { it.discipline == "Running" })
    }

    @Test
    fun disciplinesIncluyeLasFijasYLasRegistradas() = runTest {
        val service = DefaultDiscoveryService(
            FakeProfileRepository(listOf(trainer("1", "Pilates"))),
            FakeReviewService(),
        )

        val disciplines = service.disciplines().getOrThrow()

        assertTrue("Pilates" in disciplines)
        assertTrue("Yoga" in disciplines)
    }
}
