package com.caracolas.gestortrainer.feature.activities.api

import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.ExerciseMedia
import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.core.model.TrainerId
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/** NUTRITION está reservado: el flag Flag.NUTRITION está apagado en el MVP. */
enum class ActivityType { PHYSICAL, NUTRITION }

data class Activity(
    val id: ActivityId,
    val trainerId: TrainerId,
    val type: ActivityType,
    val title: String,
    val description: String,
    /** No se muestra al alumno; lo usa la puntuación. */
    val averageDuration: Duration,
    /** Vacío si type == NUTRITION. */
    val muscles: List<MuscleId>,
    /** MVP: solo videos. */
    val media: List<ExerciseMedia>,
)

data class ActivityDraft(
    val type: ActivityType = ActivityType.PHYSICAL,
    val title: String = "",
    val description: String = "",
    val averageDuration: Duration = Duration.ZERO,
    val muscles: List<MuscleId> = emptyList(),
    val media: List<ExerciseMedia> = emptyList(),
)

enum class ValidationError {
    TITLE_REQUIRED,
    DESCRIPTION_REQUIRED,
    DURATION_REQUIRED,
    MUSCLES_REQUIRED_FOR_PHYSICAL,
    MUSCLES_NOT_ALLOWED_FOR_NUTRITION,
}

/** Lo que ve el alumno de una actividad: nunca incluye [Activity.averageDuration] (la usa la puntuación). */
data class ActivityPreview(
    val id: ActivityId,
    val type: ActivityType,
    val title: String,
    val description: String,
    val muscles: List<MuscleId>,
    val media: List<ExerciseMedia>,
)

fun Activity.toClientPreview() = ActivityPreview(
    id = id,
    type = type,
    title = title,
    description = description,
    muscles = muscles,
    media = media,
)

interface ActivityRepository {
    /** Biblioteca de plantillas del trainer. */
    fun observeLibrary(trainerId: TrainerId): Flow<List<Activity>>
    suspend fun get(id: ActivityId): Result<Activity>
    suspend fun save(draft: ActivityDraft): Result<ActivityId>
    suspend fun delete(id: ActivityId): Result<Unit>
}

/** Reglas de validación separadas de la UI. */
interface ActivityValidator {
    fun validate(draft: ActivityDraft): List<ValidationError>
}
