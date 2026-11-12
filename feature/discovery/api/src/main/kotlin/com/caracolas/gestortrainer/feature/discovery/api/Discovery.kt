package com.caracolas.gestortrainer.feature.discovery.api

import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import kotlinx.coroutines.flow.Flow

data class TrainerSummary(
    val trainerId: TrainerId,
    val publicId: PublicId,
    val displayName: String,
    val mainDiscipline: String,
    val averageStars: Double,
    val region: Region,
    val yearsAsTrainer: Int,
)

data class TrainerQuery(val discipline: String? = null, val text: String? = null)

data class PopularCategory(val discipline: String, val imageUrl: String?, val trainerCount: Int)

/** Buscar actividades y trainers. Enviar la solicitud lo hace LinkService (feature:clients). */
interface DiscoveryService {
    suspend fun disciplines(): Result<List<String>>
    fun searchTrainers(query: TrainerQuery): Flow<List<TrainerSummary>>
    /** Disciplinas con más alumnos activos en los últimos 30 días. Lo calcula el backend; aquí, una lista fija. */
    fun popularCategories(limit: Int = 6): Flow<List<PopularCategory>>
}
