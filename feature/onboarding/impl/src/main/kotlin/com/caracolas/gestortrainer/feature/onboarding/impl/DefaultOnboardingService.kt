package com.caracolas.gestortrainer.feature.onboarding.impl

import com.caracolas.gestortrainer.core.auth.Session
import com.caracolas.gestortrainer.core.auth.SessionRepository
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.onboarding.api.OnboardingService
import com.caracolas.gestortrainer.feature.onboarding.api.RegistrationData
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidationError
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class OnboardingValidationException(val errors: List<ProfileValidationError>) :
    Exception("Datos de registro inválidos: $errors")

class DefaultOnboardingService @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val profileValidator: ProfileValidator,
    private val sessionRepository: SessionRepository,
) : OnboardingService {

    override val isOnboardingComplete = sessionRepository.session.map { it != null }

    override suspend fun register(data: RegistrationData): Result<UserId> {
        val bodyErrors = (data as? RegistrationData.Client)?.body?.let(profileValidator::validate).orEmpty()
        val errors = profileValidator.validate(data.personal) + bodyErrors
        if (errors.isNotEmpty()) return Result.failure(OnboardingValidationException(errors))

        val userId = UserId(UUID.randomUUID().toString())
        val role = if (data is RegistrationData.Trainer) Role.TRAINER else Role.CLIENT
        val publicId = PublicIdFactory.create(role)

        val saveResult = when (data) {
            is RegistrationData.Trainer -> profileRepository.saveTrainer(
                TrainerProfile(
                    userId = userId,
                    publicId = publicId,
                    personal = data.personal,
                    mainDiscipline = data.mainDiscipline,
                    secondaryDisciplines = data.secondaryDisciplines,
                    yearsAsTrainer = data.yearsAsTrainer,
                    bio = "",
                    photoUrl = null,
                ),
            )

            is RegistrationData.Client -> profileRepository.saveClient(
                ClientProfile(
                    userId = userId,
                    publicId = publicId,
                    personal = data.personal,
                    body = data.body,
                    photoUrl = null,
                ),
            )
        }
        saveResult.onFailure { return Result.failure(it) }

        sessionRepository.startSession(Session(userId, publicId, role)).onFailure { return Result.failure(it) }
        return Result.success(userId)
    }
}
