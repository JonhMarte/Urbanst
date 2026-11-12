package com.caracolas.gestortrainer.feature.assessment.impl

import com.caracolas.gestortrainer.core.events.AssessmentUpdated
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentRepository
import com.caracolas.gestortrainer.feature.assessment.api.ClientAssessment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Cuestionario del alumno en memoria mientras no exista el backend (tabla por clientId, cifrada). */
@Singleton
class InMemoryAssessmentRepository @Inject constructor(
    private val eventBus: EventBus,
) : AssessmentRepository {
    private val assessments = MutableStateFlow<Map<ClientId, ClientAssessment>>(emptyMap())

    override fun observe(clientId: ClientId): Flow<ClientAssessment?> = assessments.map { it[clientId] }

    override suspend fun save(assessment: ClientAssessment): Result<Unit> {
        val normalized = assessment.normalized().copy(updatedAt = Instant.now())
        assessments.value = assessments.value + (normalized.clientId to normalized)
        eventBus.publish(AssessmentUpdated(normalized.clientId, normalized.updatedAt))
        return Result.success(Unit)
    }
}
