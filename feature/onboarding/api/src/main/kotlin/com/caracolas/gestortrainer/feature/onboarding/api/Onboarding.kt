package com.caracolas.gestortrainer.feature.onboarding.api

import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.PrivateBodyData
import kotlinx.coroutines.flow.Flow

sealed interface RegistrationData {
    val personal: PersonalData

    data class Trainer(
        override val personal: PersonalData,
        val mainDiscipline: String,
        val secondaryDisciplines: List<String>,
        val yearsAsTrainer: Int,
    ) : RegistrationData

    data class Client(
        override val personal: PersonalData,
        val body: PrivateBodyData?,
    ) : RegistrationData
}

interface OnboardingService {
    suspend fun register(data: RegistrationData): Result<UserId>
    val isOnboardingComplete: Flow<Boolean>
}
