package com.caracolas.gestortrainer.feature.activities.impl

import com.caracolas.gestortrainer.core.auth.SessionRepository
import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.activities.api.Activity
import com.caracolas.gestortrainer.feature.activities.api.ActivityDraft
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Biblioteca de actividades en memoria mientras no exista el backend AWS (Fase 1). */
@Singleton
class InMemoryActivityRepository @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ActivityRepository {
    private val activities = MutableStateFlow<Map<ActivityId, Activity>>(emptyMap())

    override fun observeLibrary(trainerId: TrainerId): Flow<List<Activity>> =
        activities.map { map -> map.values.filter { it.trainerId == trainerId } }

    override suspend fun get(id: ActivityId): Result<Activity> =
        activities.value[id]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("No existe la actividad $id"))

    override suspend fun save(draft: ActivityDraft): Result<ActivityId> {
        val trainerId = currentTrainerId()
            ?: return Result.failure(IllegalStateException("No hay una sesión de trainer activa"))
        val id = ActivityId(UUID.randomUUID().toString())
        val activity = Activity(
            id = id,
            trainerId = trainerId,
            type = draft.type,
            title = draft.title,
            description = draft.description,
            averageDuration = draft.averageDuration,
            muscles = draft.muscles,
            media = draft.media,
        )
        activities.value = activities.value + (id to activity)
        return Result.success(id)
    }

    override suspend fun delete(id: ActivityId): Result<Unit> {
        activities.value = activities.value - id
        return Result.success(Unit)
    }

    private suspend fun currentTrainerId(): TrainerId? =
        sessionRepository.session.first()?.let { TrainerId(it.userId.value) }
}
