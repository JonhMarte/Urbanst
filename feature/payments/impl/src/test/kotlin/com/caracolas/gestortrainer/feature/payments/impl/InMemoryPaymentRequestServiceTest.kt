package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.PaymentRequested
import com.caracolas.gestortrainer.core.events.PaymentSucceeded
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.LinkRequestId
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.clients.api.LinkRequest
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.payments.api.BillingPeriod
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestError
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestException
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestStatus
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
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

private class FakeLinkService(private val linked: Map<ClientId, List<TrainerId>> = emptyMap()) : LinkService {
    override suspend fun requestByTrainerId(clientId: ClientId, trainerPublicId: PublicId) =
        Result.failure<LinkRequestId>(UnsupportedOperationException())
    override suspend fun addByClientId(trainerId: TrainerId, clientPublicId: PublicId) =
        Result.failure<LinkRequestId>(UnsupportedOperationException())
    override fun observeIncoming(userId: UserId) = MutableStateFlow(emptyList<LinkRequest>())
    override suspend fun accept(requestId: LinkRequestId) = Result.success(Unit)
    override suspend fun reject(requestId: LinkRequestId) = Result.success(Unit)
    override fun observeLinkedTrainers(clientId: ClientId) = MutableStateFlow(linked[clientId].orEmpty())
}

private class FakeProfileRepository(private val clients: Map<PublicId, ClientProfile>) : ProfileRepository {
    override fun observeTrainer(id: TrainerId) = MutableStateFlow<TrainerProfile?>(null)
    override fun observeClient(id: ClientId) = MutableStateFlow(clients.values.find { it.userId.value == id.value })
    override suspend fun saveTrainer(profile: TrainerProfile) = Result.success(Unit)
    override suspend fun saveClient(profile: ClientProfile) = Result.success(Unit)
    override suspend fun findTrainerByPublicId(publicId: PublicId) = null
    override suspend fun findClientByPublicId(publicId: PublicId) = clients[publicId]
    override fun observeAllTrainers() = MutableStateFlow(emptyList<TrainerProfile>())
    override fun observeAllClients() = MutableStateFlow(clients.values.toList())
}

private fun client(id: String, publicId: String) = ClientProfile(
    userId = UserId(id),
    publicId = PublicId(publicId),
    personal = PersonalData("Cliente $id", "Española", Region("ES"), age = 25),
    body = null,
    photoUrl = null,
)

class InMemoryPaymentRequestServiceTest {
    private val trainerId = TrainerId("t1")
    private val clientId = ClientId("c1")
    private val clientPublicId = PublicId("AL-1")
    private val eventBus = InMemoryEventBus()
    private val linkService = FakeLinkService(mapOf(clientId to listOf(trainerId)))
    private val profileRepository = FakeProfileRepository(mapOf(clientPublicId to client("c1", "AL-1")))
    private val router = DefaultPaymentRouter(FakePaymentGateway())
    private val service = InMemoryPaymentRequestService(linkService, profileRepository, router, eventBus)

    @Test
    fun guardarBorradorParaUnAlumnoNoVinculadoFalla() = runTest {
        val result = service.saveDraft(TrainerId("otro"), clientPublicId, BillingPeriod.MONTHLY, Money(1000, "EUR"))
        assertTrue(result.isFailure)
        assertEquals(PaymentRequestError.CLIENT_NOT_LINKED, (result.exceptionOrNull() as PaymentRequestException).error)
    }

    @Test
    fun guardarBorradorConMontoInvalidoFalla() = runTest {
        val result = service.saveDraft(trainerId, clientPublicId, BillingPeriod.MONTHLY, Money(0, "EUR"))
        assertTrue(result.isFailure)
    }

    @Test
    fun enviarUnaSolicitudLaDejaPendienteYPublicaElEvento() = runTest(UnconfinedTestDispatcher()) {
        val id = service.saveDraft(trainerId, clientPublicId, BillingPeriod.MONTHLY, Money(1000, "EUR")).getOrThrow()
        val received = async { eventBus.on<PaymentRequested>().first() }

        service.send(id)

        val pending = service.observePendingForClient(clientId).first()
        assertEquals(1, pending.size)
        assertEquals(PaymentRequestStatus.SENT, pending.first().status)
        assertEquals(id, received.await().id)
    }

    @Test
    fun enviarUnaNuevaSolicitudCancelaLaAnteriorDelMismoAlumno() = runTest {
        val first = service.saveDraft(trainerId, clientPublicId, BillingPeriod.MONTHLY, Money(1000, "EUR")).getOrThrow()
        service.send(first)
        val second = service.saveDraft(trainerId, clientPublicId, BillingPeriod.DAILY, Money(500, "EUR")).getOrThrow()

        service.send(second)

        val forTrainer = service.observeForTrainer(trainerId).first()
        assertEquals(PaymentRequestStatus.CANCELLED, forTrainer.first { it.id == first }.status)
        assertEquals(PaymentRequestStatus.SENT, forTrainer.first { it.id == second }.status)
    }

    @Test
    fun pagarUnaSolicitudEnviadaLaActivaYOtorgaElEntitlement() = runTest(UnconfinedTestDispatcher()) {
        val id = service.saveDraft(trainerId, clientPublicId, BillingPeriod.MONTHLY, Money(1000, "EUR")).getOrThrow()
        service.send(id)
        val received = async { eventBus.on<PaymentSucceeded>().first() }

        val result = service.pay(id)

        assertTrue(result is PaymentResult.Success)
        val requests = service.observeForTrainer(trainerId).first()
        assertEquals(PaymentRequestStatus.PAID, requests.first { it.id == id }.status)
        val entitlements = service.observe(UserId(clientId.value)).first()
        assertEquals(1, entitlements.size)
        assertEquals(UserId(clientId.value), received.await().userId)
    }

    @Test
    fun noSePuedePagarUnaSolicitudQueNoSeHaEnviado() = runTest {
        val id = service.saveDraft(trainerId, clientPublicId, BillingPeriod.MONTHLY, Money(1000, "EUR")).getOrThrow()

        val result = service.pay(id)

        assertTrue(result is PaymentResult.Failed)
    }
}
