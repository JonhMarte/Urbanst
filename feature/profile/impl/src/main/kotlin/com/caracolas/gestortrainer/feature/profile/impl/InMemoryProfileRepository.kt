package com.caracolas.gestortrainer.feature.profile.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Caché en memoria mientras no exista el backend AWS (Fase 1). */
@Singleton
class InMemoryProfileRepository @Inject constructor() : ProfileRepository {
    private val trainers = MutableStateFlow<Map<TrainerId, TrainerProfile>>(emptyMap())
    private val clients = MutableStateFlow<Map<ClientId, ClientProfile>>(emptyMap())

    override fun observeTrainer(id: TrainerId): Flow<TrainerProfile?> = trainers.map { it[id] }
    override fun observeClient(id: ClientId): Flow<ClientProfile?> = clients.map { it[id] }

    override suspend fun saveTrainer(profile: TrainerProfile): Result<Unit> {
        trainers.value = trainers.value + (TrainerId(profile.userId.value) to profile)
        return Result.success(Unit)
    }

    override suspend fun saveClient(profile: ClientProfile): Result<Unit> {
        clients.value = clients.value + (ClientId(profile.userId.value) to profile)
        return Result.success(Unit)
    }

    override suspend fun findTrainerByPublicId(publicId: PublicId): TrainerProfile? =
        trainers.value.values.find { it.publicId == publicId }

    override suspend fun findClientByPublicId(publicId: PublicId): ClientProfile? =
        clients.value.values.find { it.publicId == publicId }

    override fun observeAllTrainers(): Flow<List<TrainerProfile>> = trainers.map { it.values.toList() }
    override fun observeAllClients(): Flow<List<ClientProfile>> = clients.map { it.values.toList() }
}
