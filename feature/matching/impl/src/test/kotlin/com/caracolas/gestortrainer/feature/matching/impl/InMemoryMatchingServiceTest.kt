package com.caracolas.gestortrainer.feature.matching.impl

import com.caracolas.gestortrainer.core.events.ClientLinked
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.matching.api.MatchingError
import com.caracolas.gestortrainer.feature.matching.api.MatchingException
import com.caracolas.gestortrainer.feature.matching.api.Proposal
import com.caracolas.gestortrainer.feature.matching.api.ProposalPayment
import com.caracolas.gestortrainer.feature.matching.api.RequestGoal
import com.caracolas.gestortrainer.feature.matching.api.ShareFlags
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequestStatus
import com.caracolas.gestortrainer.feature.payments.api.BillingPeriod
import com.caracolas.gestortrainer.feature.payments.api.CheckoutRequest
import com.caracolas.gestortrainer.feature.payments.api.Delivery
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PaymentGateway
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
import com.caracolas.gestortrainer.feature.payments.api.PaymentRouter
import com.caracolas.gestortrainer.feature.payments.api.Product
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
import java.time.Instant

private class FakeProfileRepository(private val trainer: TrainerProfile) : ProfileRepository {
    override fun observeTrainer(id: TrainerId) = MutableStateFlow(if (id.value == trainer.userId.value) trainer else null)
    override fun observeClient(id: ClientId) = MutableStateFlow<ClientProfile?>(null)
    override suspend fun saveTrainer(profile: TrainerProfile) = Result.success(Unit)
    override suspend fun saveClient(profile: ClientProfile) = Result.success(Unit)
    override suspend fun findTrainerByPublicId(publicId: PublicId) = if (publicId == trainer.publicId) trainer else null
    override suspend fun findClientByPublicId(publicId: PublicId) = null
    override fun observeAllTrainers() = MutableStateFlow(listOf(trainer))
    override fun observeAllClients() = MutableStateFlow(emptyList<ClientProfile>())
}

private class FakeGateway(private val result: PaymentResult) : PaymentGateway {
    override val id = GatewayId.STRIPE
    override suspend fun checkout(request: CheckoutRequest) = result
}

private class FakeRouter(private val gateway: PaymentGateway) : PaymentRouter {
    override fun gatewayFor(product: Product, region: Region, delivery: Delivery) = gateway
}

private fun trainer() = TrainerProfile(
    userId = UserId("t1"),
    publicId = PublicId("TR-1"),
    personal = PersonalData("Laura", "Española", Region("ES"), age = 30),
    mainDiscipline = "Yoga",
    secondaryDisciplines = emptyList(),
    yearsAsTrainer = 5,
    bio = "Hola",
    photoUrl = null,
)

class InMemoryMatchingServiceTest {
    private val trainerProfile = trainer()
    private val profileRepository = FakeProfileRepository(trainerProfile)
    private val eventBus = InMemoryEventBus()
    private val clientId = ClientId("c1")

    private fun service(result: PaymentResult = PaymentResult.Success("pay_1")) =
        InMemoryMatchingService(profileRepository, FakeRouter(FakeGateway(result)), eventBus)

    @Test
    fun enviarSolicitudAUnTrainerDesconocidoFalla() = runTest {
        val result = service().sendRequest(clientId, PublicId("TR-999"), "hola", emptyList(), ShareFlags())
        assertTrue(result.isFailure)
        assertEquals(MatchingError.TRAINER_NOT_FOUND, (result.exceptionOrNull() as MatchingException).error)
    }

    @Test
    fun enviarSolicitudValidaQuedaEnNuevas() = runTest {
        val service = service()
        val id = service.sendRequest(clientId, trainerProfile.publicId, "Quiero mejorar mi fuerza", listOf(RequestGoal.FUERZA), ShareFlags()).getOrThrow()

        val forTrainer = service.observeForTrainer(TrainerId("t1")).first()
        assertEquals(1, forTrainer.size)
        assertEquals(TrainerRequestStatus.NEW, forTrainer.first { it.id == id }.status)
    }

    @Test
    fun rechazarUnaSolicitudLaMarcaRechazada() = runTest {
        val service = service()
        val id = service.sendRequest(clientId, trainerProfile.publicId, "hola", emptyList(), ShareFlags()).getOrThrow()

        service.reject(id)

        val forClient = service.observeForClient(clientId).first()
        assertEquals(TrainerRequestStatus.REJECTED, forClient.first { it.id == id }.status)
    }

    @Test
    fun enviarPropuestaAUnaSolicitudYaRespondidaFalla() = runTest {
        val service = service()
        val id = service.sendRequest(clientId, trainerProfile.publicId, "hola", emptyList(), ShareFlags()).getOrThrow()
        service.reject(id)

        val result = service.sendProposal(id, proposal())

        assertTrue(result.isFailure)
        assertEquals(MatchingError.NOT_IN_NEW_STATUS, (result.exceptionOrNull() as MatchingException).error)
    }

    @Test
    fun aceptarYPagarUnaPropuestaVinculaAlAlumnoYPublicaElEvento() = runTest(UnconfinedTestDispatcher()) {
        val service = service()
        val id = service.sendRequest(clientId, trainerProfile.publicId, "hola", emptyList(), ShareFlags()).getOrThrow()
        service.sendProposal(id, proposal())
        val received = async { eventBus.on<ClientLinked>().first() }

        val result = service.acceptAndPay(id)

        assertTrue(result is PaymentResult.Success)
        val forClient = service.observeForClient(clientId).first()
        assertEquals(TrainerRequestStatus.ACCEPTED, forClient.first { it.id == id }.status)
        val event = received.await()
        assertEquals(TrainerId("t1"), event.trainerId)
        assertEquals(clientId, event.clientId)
    }

    @Test
    fun aceptarYPagarSinPropuestaFalla() = runTest {
        val service = service()
        val id = service.sendRequest(clientId, trainerProfile.publicId, "hola", emptyList(), ShareFlags()).getOrThrow()

        val result = service.acceptAndPay(id)

        assertTrue(result is PaymentResult.Failed)
    }

    @Test
    fun siElPagoFallaLaSolicitudSigueConPropuestaEnviada() = runTest {
        val service = service(result = PaymentResult.Failed("tarjeta rechazada"))
        val id = service.sendRequest(clientId, trainerProfile.publicId, "hola", emptyList(), ShareFlags()).getOrThrow()
        service.sendProposal(id, proposal())

        service.acceptAndPay(id)

        val forClient = service.observeForClient(clientId).first()
        assertEquals(TrainerRequestStatus.PROPOSAL_SENT, forClient.first { it.id == id }.status)
    }

    @Test
    fun actualizarLoQueComparteElAlumno() = runTest {
        val service = service()
        val id = service.sendRequest(clientId, trainerProfile.publicId, "hola", emptyList(), ShareFlags()).getOrThrow()

        service.updateShareFlags(id, ShareFlags(questionnaireShared = true))

        val forClient = service.observeForClient(clientId).first()
        assertTrue(forClient.first { it.id == id }.shareFlags.questionnaireShared)
    }

    private fun proposal() = Proposal(
        title = "Plan de 3 meses",
        description = "Fuerza e hipertrofia",
        durationLabel = "3 meses",
        payment = ProposalPayment.Recurring(BillingPeriod.MONTHLY, Money(2500, "EUR")),
        expiresAt = Instant.now().plusSeconds(7 * 24 * 3600),
    )
}
