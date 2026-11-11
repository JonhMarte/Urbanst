package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.feature.scoring.api.TrainerRankingPolicy
import com.caracolas.gestortrainer.feature.scoring.api.TrainerStats
import javax.inject.Inject

/**
 * Puntuación del trainer (0–100):
 * - 50 %: estrellas con media bayesiana (pocas valoraciones pesan menos),
 * - 30 %: cumplimiento de sus alumnos,
 * - 20 %: retención a 30 días.
 */
class BayesianTrainerRankingPolicy @Inject constructor() : TrainerRankingPolicy {
    override fun score(stats: TrainerStats): Double {
        val bayesStars = (PRIOR_WEIGHT * PRIOR_MEAN + stats.reviewCount * stats.averageStars) /
            (PRIOR_WEIGHT + stats.reviewCount)
        val starsPart = (bayesStars / 5.0).coerceIn(0.0, 1.0)
        val completion = stats.clientCompletionRate.coerceIn(0.0, 1.0)
        val retention = stats.retention30d.coerceIn(0.0, 1.0)
        return 100.0 * (0.5 * starsPart + 0.3 * completion + 0.2 * retention)
    }

    companion object {
        const val PRIOR_MEAN = 3.5
        const val PRIOR_WEIGHT = 10.0
    }
}
