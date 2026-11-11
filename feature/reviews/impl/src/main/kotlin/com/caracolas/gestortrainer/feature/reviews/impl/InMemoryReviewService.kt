package com.caracolas.gestortrainer.feature.reviews.impl

import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.events.ReviewSubmitted
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.reviews.api.Review
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import com.caracolas.gestortrainer.feature.reviews.api.ReviewSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private data class StoredReview(
    val trainerId: TrainerId,
    val authorDisplayName: String,
    val stars: Int,
    val comment: String?,
    val at: Instant,
)

/** Valoraciones en memoria mientras no exista el backend (Fase 2). */
@Singleton
class InMemoryReviewService @Inject constructor(
    private val linkService: LinkService,
    private val profileRepository: ProfileRepository,
    private val eventBus: EventBus,
) : ReviewService {
    private val reviews = MutableStateFlow<List<StoredReview>>(emptyList())

    override suspend fun rate(clientId: ClientId, trainerId: TrainerId, stars: Int, comment: String?): Result<Unit> {
        if (stars !in 1..5) return Result.failure(IllegalArgumentException("Las estrellas van de 1 a 5"))
        val linked = linkService.observeLinkedTrainers(clientId).first().contains(trainerId)
        if (!linked) return Result.failure(IllegalStateException("Solo puedes valorar a tu trainer"))

        val authorDisplayName = profileRepository.observeClient(clientId).first()?.personal?.fullName ?: clientId.value
        val now = Instant.now()
        reviews.value = reviews.value + StoredReview(trainerId, authorDisplayName, stars, comment, now)
        eventBus.publish(ReviewSubmitted(trainerId, clientId, stars, now))
        return Result.success(Unit)
    }

    override fun observe(trainerId: TrainerId): Flow<ReviewSummary> = reviews.map { all ->
        val mine = all.filter { it.trainerId == trainerId }
        val average = if (mine.isEmpty()) 0.0 else mine.map { it.stars }.average()
        ReviewSummary(
            trainerId = trainerId,
            averageStars = average,
            count = mine.size,
            recent = mine.takeLast(RECENT_LIMIT).reversed().map { Review(it.authorDisplayName, it.stars, it.comment, it.at) },
        )
    }

    companion object {
        private const val RECENT_LIMIT = 10
    }
}
