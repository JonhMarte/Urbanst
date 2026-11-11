package com.caracolas.gestortrainer.feature.clients.impl

import com.caracolas.gestortrainer.core.events.ClientLinked
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.LinkRequested
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.clients.api.LinkError
import com.caracolas.gestortrainer.feature.clients.api.LinkException
import com.caracolas.gestortrainer.feature.clients.api.LinkStatus
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

private class FakeProfileRepository : ProfileRepository {
    val trainers = mutableMapOf<TrainerId, TrainerProfile>()
    val clients = mutableMapOf<ClientId, ClientProfile>()

    override fun observeTrainer(id: TrainerId) = MutableStateFlow(trainers[id])
    override fun observeClient(id: ClientId) = MutableStateFlow(clients[id])
    override suspend fun saveTrainer(profile: TrainerProfile): Result<Unit> {
        trainers[TrainerId(profile.userId.value)] = profile
        return Result.success(Unit)
    }

    override suspend fun saveClient(profile: ClientProfile): Result<Unit> {
        clients[ClientId(profile.userId.value)] = profile
        return Result.success(Unit)
    }

    override suspend fun findTrainerByPublicId(publicId: PublicId) = trainers.values.find { it.publicId == publicId }
    override suspend fun findClientByPublicId(publicId: PublicId) = clients.values.find { it.publicId == publicId }
    override fun observeAllTrainers() = MutableStateFlow(trainers.values.toList())
    override fun observeAllClients() = MutableStateFlow(clients.values.toList())
}

private fun trainer(id: String, publicId: String) = TrainerProfile(
    userId = UserId(id),
    publicId = PublicId(publicId),
    personal = PersonalData("Trainer $id", "Española", Region("ES"), age = 34),
    mainDiscipline = "Gym",
    secondaryDisciplines = emptyList(),
    yearsAsTrainer = 5,
    bio = "",
    photoUrl = null,
)

private fun client(id: String, publicId: String) = ClientProfile(
    userId = UserId(id),
    publicId = PublicId(publicId),
    personal = PersonalData("Cliente $id", "Española", Region("ES"), age = 29),
    body = null,
    photoUrl = null,
)

class DefaultLinkServiceTest {
    private val profileRepository = FakeProfileRepository().apply {
        trainers[TrainerId("t1")] = trainer("t1", "TR-00001")
        clients[ClientId("c1")] = client("c1", "AL-00001")
    }
    private val eventBus = InMemoryEventBus()
    private val service = DefaultLinkService(profileRepository, eventBus)

    @Test
    fun unIdDeTrainerInexistenteFalla() = runTest {
        val result = service.requestByTrainerId(ClientId("c1"), PublicId("TR-99999"))
        assertTrue(result.isFailure)
        assertEquals(LinkError.TRAINER_NOT_FOUND, (result.exceptionOrNull() as LinkException).error)
    }

    @Test
    fun unIdDeClienteInexistenteFalla() = runTest {
        val result = service.addByClientId(TrainerId("t1"), PublicId("AL-99999"))
        assertTrue(result.isFailure)
        assertEquals(LinkError.CLIENT_NOT_FOUND, (result.exceptionOrNull() as LinkException).error)
    }

    @Test
    fun unaSolicitudValidaQuedaPendienteYPublicaElEvento() = runTest(UnconfinedTestDispatcher()) {
        val received = async { eventBus.on<LinkRequested>().first() }

        val result = service.addByClientId(TrainerId("t1"), PublicId("AL-00001"))

        assertTrue(result.isSuccess)
        val incoming = service.observeIncoming(UserId("c1")).first()
        assertEquals(1, incoming.size)
        assertEquals(LinkStatus.PENDING, incoming.first().status)
        assertEquals(TrainerId("t1"), received.await().trainerId)
    }

    @Test
    fun noSePuedeEnviarUnaSegundaSolicitudMientrasHayUnaPendiente() = runTest {
        service.addByClientId(TrainerId("t1"), PublicId("AL-00001"))

        val result = service.addByClientId(TrainerId("t1"), PublicId("AL-00001"))

        assertTrue(result.isFailure)
        assertEquals(LinkError.REQUEST_ALREADY_PENDING, (result.exceptionOrNull() as LinkException).error)
    }

    @Test
    fun aceptarUnaSolicitudVinculaYPublicaClientLinked() = runTest(UnconfinedTestDispatcher()) {
        val received = async { eventBus.on<ClientLinked>().first() }
        val requestId = service.addByClientId(TrainerId("t1"), PublicId("AL-00001")).getOrThrow()

        val result = service.accept(requestId)

        assertTrue(result.isSuccess)
        assertEquals(TrainerId("t1"), received.await().trainerId)
        assertTrue(service.observeIncoming(UserId("c1")).first().isEmpty())
    }

    @Test
    fun noSePuedeVolverAVincularUnaPautaYaAceptada() = runTest {
        val requestId = service.addByClientId(TrainerId("t1"), PublicId("AL-00001")).getOrThrow()
        service.accept(requestId)

        val result = service.addByClientId(TrainerId("t1"), PublicId("AL-00001"))

        assertTrue(result.isFailure)
        assertEquals(LinkError.ALREADY_LINKED, (result.exceptionOrNull() as LinkException).error)
    }

    @Test
    fun rechazarUnaSolicitudLaQuitaDeLasPendientesSinVincular() = runTest {
        val requestId = service.addByClientId(TrainerId("t1"), PublicId("AL-00001")).getOrThrow()

        val result = service.reject(requestId)

        assertTrue(result.isSuccess)
        assertTrue(service.observeIncoming(UserId("c1")).first().isEmpty())
        assertTrue(service.acceptedLinksFor(TrainerId("t1")).first().isEmpty())
    }

    @Test
    fun observeIncomingSoloDevuelveLoQueCorrespondeAlDestinatario() = runTest {
        // Trainer -> cliente: pendiente la ve el cliente, no el trainer.
        service.addByClientId(TrainerId("t1"), PublicId("AL-00001"))
        assertTrue(service.observeIncoming(UserId("t1")).first().isEmpty())
        assertEquals(1, service.observeIncoming(UserId("c1")).first().size)
    }
}
