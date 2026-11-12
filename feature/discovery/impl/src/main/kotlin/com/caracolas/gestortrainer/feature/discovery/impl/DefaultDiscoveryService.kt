package com.caracolas.gestortrainer.feature.discovery.impl

import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.discovery.api.DiscoveryService
import com.caracolas.gestortrainer.feature.discovery.api.PopularCategory
import com.caracolas.gestortrainer.feature.discovery.api.TrainerQuery
import com.caracolas.gestortrainer.feature.discovery.api.TrainerSummary
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Búsqueda de trainers y categorías populares en memoria mientras no exista el backend. */
class DefaultDiscoveryService @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val reviewService: ReviewService,
) : DiscoveryService {

    override suspend fun disciplines(): Result<List<String>> {
        val registered = profileRepository.observeAllTrainers().first().map { it.mainDiscipline }
        return Result.success((FIXED_DISCIPLINES + registered).filter { it.isNotBlank() }.distinct())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun searchTrainers(query: TrainerQuery): Flow<List<TrainerSummary>> =
        profileRepository.observeAllTrainers().flatMapLatest { trainers ->
            val discipline = query.discipline
            val text = query.text
            val filtered = trainers.filter { trainer ->
                val matchesDiscipline = discipline == null ||
                    trainer.mainDiscipline.equals(discipline, ignoreCase = true) ||
                    trainer.secondaryDisciplines.any { it.equals(discipline, ignoreCase = true) }
                val matchesText = text.isNullOrBlank() ||
                    trainer.personal.fullName.contains(text, ignoreCase = true)
                matchesDiscipline && matchesText
            }
            if (filtered.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(filtered.map { trainerSummary(it) }) { it.toList() }
            }
        }

    override fun popularCategories(limit: Int): Flow<List<PopularCategory>> =
        profileRepository.observeAllTrainers().map { trainers ->
            FIXED_DISCIPLINES
                .map { name -> PopularCategory(name, null, trainers.count { it.mainDiscipline.equals(name, ignoreCase = true) }) }
                .sortedByDescending { it.trainerCount }
                .take(limit)
        }

    private fun trainerSummary(trainer: TrainerProfile): Flow<TrainerSummary> =
        reviewService.observe(TrainerId(trainer.userId.value)).map { summary ->
            TrainerSummary(
                trainerId = TrainerId(trainer.userId.value),
                publicId = trainer.publicId,
                displayName = trainer.personal.fullName,
                mainDiscipline = trainer.mainDiscipline,
                averageStars = summary.averageStars,
                region = trainer.personal.region,
                yearsAsTrainer = trainer.yearsAsTrainer,
            )
        }

    companion object {
        private val FIXED_DISCIPLINES = listOf("Gimnasio", "Running", "Yoga", "CrossFit", "Natación", "Ciclismo")
    }
}
