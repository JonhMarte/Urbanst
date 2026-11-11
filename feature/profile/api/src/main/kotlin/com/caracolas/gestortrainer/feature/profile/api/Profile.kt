package com.caracolas.gestortrainer.feature.profile.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.flow.Flow

data class PersonalData(
    val fullName: String,
    val nationality: String,
    val region: Region,
    val age: Int?,
    /** Solo relevante si [age] indica un menor de edad (control de edad en el registro). */
    val guardianConsentGiven: Boolean = false,
)

data class TrainerProfile(
    val userId: UserId,
    val publicId: PublicId,
    val personal: PersonalData,
    val mainDiscipline: String,
    val secondaryDisciplines: List<String>,
    val yearsAsTrainer: Int,
    val bio: String,
    val photoUrl: String?,
)

/** Datos corporales: privados, solo los ve el trainer vinculado y nunca salen en clasificaciones. */
data class PrivateBodyData(
    val weightKg: Double,
    val heightCm: Int,
    val consentGiven: Boolean,
)

data class ClientProfile(
    val userId: UserId,
    val publicId: PublicId,
    val personal: PersonalData,
    val body: PrivateBodyData?,
    val photoUrl: String?,
)

interface ProfileRepository {
    fun observeTrainer(id: TrainerId): Flow<TrainerProfile?>
    fun observeClient(id: ClientId): Flow<ClientProfile?>
    suspend fun saveTrainer(profile: TrainerProfile): Result<Unit>
    suspend fun saveClient(profile: ClientProfile): Result<Unit>

    /** Para vincularse por ID público (ADD New User). */
    suspend fun findTrainerByPublicId(publicId: PublicId): TrainerProfile?
    suspend fun findClientByPublicId(publicId: PublicId): ClientProfile?

    /** Para las clasificaciones globales. */
    fun observeAllTrainers(): Flow<List<TrainerProfile>>
    fun observeAllClients(): Flow<List<ClientProfile>>
}

enum class ProfileValidationError {
    FULL_NAME_REQUIRED,
    NATIONALITY_REQUIRED,
    AGE_REQUIRED,
    MINOR_REQUIRES_GUARDIAN_CONSENT,
    BODY_DATA_REQUIRES_CONSENT,
    INVALID_WEIGHT,
    INVALID_HEIGHT,
}

/** Reglas de validación separadas de la UI. */
interface ProfileValidator {
    fun validate(personal: PersonalData): List<ProfileValidationError>
    fun validate(body: PrivateBodyData): List<ProfileValidationError>
}
