package com.caracolas.gestortrainer.feature.onboarding.impl

import com.caracolas.gestortrainer.core.auth.InMemorySessionRepository
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.onboarding.api.RegistrationData
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.PrivateBodyData
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidationError
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeProfileRepository : ProfileRepository {
    var savedTrainer: TrainerProfile? = null
    var savedClient: ClientProfile? = null

    override fun observeTrainer(id: TrainerId) = MutableStateFlow(savedTrainer)
    override fun observeClient(id: ClientId) = MutableStateFlow(savedClient)

    override suspend fun saveTrainer(profile: TrainerProfile): Result<Unit> {
        savedTrainer = profile
        return Result.success(Unit)
    }

    override suspend fun saveClient(profile: ClientProfile): Result<Unit> {
        savedClient = profile
        return Result.success(Unit)
    }

    override suspend fun findTrainerByPublicId(publicId: PublicId) = savedTrainer
    override suspend fun findClientByPublicId(publicId: PublicId) = savedClient
    override fun observeAllTrainers() = MutableStateFlow(listOfNotNull(savedTrainer))
    override fun observeAllClients() = MutableStateFlow(listOfNotNull(savedClient))
}

private class FakeProfileValidator(private val errors: List<ProfileValidationError>) : ProfileValidator {
    override fun validate(personal: PersonalData) = errors
    override fun validate(body: PrivateBodyData) = emptyList<ProfileValidationError>()
}

class DefaultOnboardingServiceTest {
    private val profileRepository = FakeProfileRepository()
    private val sessionRepository = InMemorySessionRepository()

    @Test
    fun registroValidoGuardaElPerfilYAbreSesion() = runBlocking {
        val service = DefaultOnboardingService(profileRepository, FakeProfileValidator(emptyList()), sessionRepository)
        val data = RegistrationData.Trainer(
            personal = PersonalData("Carlos Méndez", "Española", Region("ES"), age = 34),
            mainDiscipline = "Jiu-jitsu brasileño",
            secondaryDisciplines = listOf("Gym"),
            yearsAsTrainer = 5,
        )

        val result = service.register(data)

        assertTrue(result.isSuccess)
        assertEquals("Carlos Méndez", profileRepository.savedTrainer?.personal?.fullName)
        assertEquals(Role.TRAINER, sessionRepository.session.value?.role)
    }

    @Test
    fun registroConDatosInvalidosNoGuardaNadaNiAbreSesion() = runBlocking {
        val service = DefaultOnboardingService(
            profileRepository,
            FakeProfileValidator(listOf(ProfileValidationError.FULL_NAME_REQUIRED)),
            sessionRepository,
        )
        val data = RegistrationData.Client(
            personal = PersonalData("", "Española", Region("ES"), age = 29),
            body = null,
        )

        val result = service.register(data)

        assertTrue(result.isFailure)
        assertNull(profileRepository.savedClient)
        assertNull(sessionRepository.session.value)
    }
}
