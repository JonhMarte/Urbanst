package com.caracolas.gestortrainer.feature.reviews.impl

import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.ReviewSubmitted
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.LinkRequestId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.clients.api.LinkRequest
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeLinkService(private val linked: Map<ClientId, List<TrainerId>>) : LinkService {
    override suspend fun requestByTrainerId(clientId: ClientId, trainerPublicId: PublicId) =
        Result.failure<LinkRequestId>(UnsupportedOperationException())
    override suspend fun addByClientId(trainerId: TrainerId, clientPublicId: PublicId) =
        Result.failure<LinkRequestId>(UnsupportedOperationException())
    override fun observeIncoming(userId: UserId) = MutableStateFlow(emptyList<LinkRequest>())
    override suspend fun accept(requestId: LinkRequestId) = Result.success(Unit)
    override suspend fun reject(requestId: LinkRequestId) = Result.success(Unit)
    override fun observeLinkedTrainers(clientId: ClientId) = MutableStateFlow(linked[clientId].orEmpty())
}

private class FakeProfileRepository(private val clients: Map<ClientId, ClientProfile>) : ProfileRepository {
    override fun observeTrainer(id: TrainerId) = MutableStateFlow<TrainerProfile?>(null)
    override fun observeClient(id: ClientId) = MutableStateFlow(clients[id])
    override suspend fun saveTrainer(profile: TrainerProfile) = Result.success(Unit)
    override suspend fun saveClient(profile: ClientProfile) = Result.success(Unit)
    override suspend fun findTrainerByPublicId(publicId: PublicId) = null
    override suspend fun findClientByPublicId(publicId: PublicId) = null
    override fun observeAllTrainers() = MutableStateFlow(emptyList<TrainerProfile>())
    override fun observeAllClients() = MutableStateFlow(clients.values.toList())
}

private fun client(id: String) = ClientProfile(
    userId = UserId(id),
    publicId = PublicId("AL-$id"),
    personal = PersonalData("Cliente $id", "Española", Region("ES"), age = 25),
    body = null,
    photoUrl = null,
)

class InMemoryReviewServiceTest {
    private val clientId = ClientId("c1")
    private val trainerId = TrainerId("t1")
    private val linkService = FakeLinkService(mapOf(clientId to listOf(trainerId)))
    private val profileRepository = FakeProfileRepository(mapOf(clientId to client("c1")))
    private val eventBus = InMemoryEventBus()
    private val service = InMemoryReviewService(linkService, profileRepository, eventBus)

    @Test
    fun valorarUnTrainerVinculadoQuedaRegistradoYPublicaElEvento() = runTest(UnconfinedTestDispatcher()) {
        val received = async { eventBus.on<ReviewSubmitted>().first() }

        val result = service.rate(clientId, trainerId, 5, "Genial")

        assertTrue(result.isSuccess)
        val summary = service.observe(trainerId).first()
        assertEquals(1, summary.count)
        assertEquals(5.0, summary.averageStars, 0.0)
        assertEquals(5, received.await().stars)
    }

    @Test
    fun valorarUnTrainerNoVinculadoFalla() = runTest {
        val result = service.rate(clientId, TrainerId("otro"), 4, null)
        assertTrue(result.isFailure)
    }

    @Test
    fun estrellasFueraDeRangoFallan() = runTest {
        val result = service.rate(clientId, trainerId, 6, null)
        assertTrue(result.isFailure)
    }

    @Test
    fun laMediaSeCalculaSobreTodasLasValoraciones() = runTest {
        service.rate(clientId, trainerId, 4, null)
        service.rate(clientId, trainerId, 2, null)

        val summary = service.observe(trainerId).first()

        assertEquals(2, summary.count)
        assertEquals(3.0, summary.averageStars, 0.0)
    }
}
