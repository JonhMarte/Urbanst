package com.caracolas.gestortrainer.feature.reviews.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.TrainerId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class Review(val authorDisplayName: String, val stars: Int, val comment: String?, val at: Instant)

data class ReviewSummary(
    val trainerId: TrainerId,
    val averageStars: Double,
    val count: Int,
    val recent: List<Review>,
)

/** Solo pueden valorar los alumnos vinculados a ese trainer. */
interface ReviewService {
    suspend fun rate(clientId: ClientId, trainerId: TrainerId, stars: Int, comment: String?): Result<Unit>
    fun observe(trainerId: TrainerId): Flow<ReviewSummary>
}
