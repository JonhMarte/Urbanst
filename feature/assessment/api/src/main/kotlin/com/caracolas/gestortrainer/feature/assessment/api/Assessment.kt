package com.caracolas.gestortrainer.feature.assessment.api

import com.caracolas.gestortrainer.core.model.ClientId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

enum class FitnessGoal { STRENGTH, FLEXIBILITY, TONING, WEIGHT_LOSS, HYPERTROPHY, ENDURANCE }
// Fuerza, Flexibilidad, Tonificación, Bajar de peso, Hipertrofia (masa muscular), Resistencia

enum class BodyArea { SHOULDERS, CHEST, ARMS, CORE, BACK, GLUTES, LEGS, CALVES }
// Hombros, Pecho, Brazos, Abdomen, Espalda, Glúteos, Piernas, Gemelos

enum class Obstacle { TIREDNESS, LACK_OF_MOTIVATION, TOO_MUCH_WORK, LITTLE_TIME }
// Cansancio, Falta de motivación, Mucho trabajo, Poco tiempo

enum class DailyTime(val minutes: Int) { MIN_15(15), MIN_30(30), HOUR_1(60), HOURS_2(120) }

data class ClientAssessment(
    val clientId: ClientId,
    val goals: Set<FitnessGoal> = emptySet(),
    val bodyAreas: Set<BodyArea> = emptySet(),
    val activityFrequency: Int = 0,
    /** Solo si activityFrequency < 4; si no, vacío. */
    val obstacles: Set<Obstacle> = emptySet(),
    val dailyTime: DailyTime? = null,
    val daysPerWeek: Int? = null,
    val psychomotorIssue: Boolean = false,
    /** Obligatorio si psychomotorIssue. Dato de salud: cifrado en reposo en el backend. */
    val psychomotorDetails: String? = null,
    val email: String = "",
    val updatedAt: Instant = Instant.EPOCH,
)

enum class AssessmentError {
    ACTIVITY_FREQUENCY_OUT_OF_RANGE,
    DAYS_PER_WEEK_OUT_OF_RANGE,
    PSYCHOMOTOR_DETAILS_REQUIRED,
    PSYCHOMOTOR_DETAILS_TOO_LONG,
    INVALID_EMAIL,
}

interface AssessmentRepository {
    fun observe(clientId: ClientId): Flow<ClientAssessment?>
    suspend fun save(assessment: ClientAssessment): Result<Unit>
}

/** Reglas de validación separadas de la UI. */
interface AssessmentValidator {
    fun validate(assessment: ClientAssessment): List<AssessmentError>
}
