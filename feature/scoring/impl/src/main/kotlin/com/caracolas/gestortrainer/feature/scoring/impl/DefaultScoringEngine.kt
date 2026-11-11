package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.core.events.DomainEvent
import com.caracolas.gestortrainer.feature.scoring.api.Points
import com.caracolas.gestortrainer.feature.scoring.api.ScoreEntry
import com.caracolas.gestortrainer.feature.scoring.api.ScoringContext
import com.caracolas.gestortrainer.feature.scoring.api.ScoringEngine
import com.caracolas.gestortrainer.feature.scoring.api.ScoringRule
import javax.inject.Inject

/**
 * Aplica todas las reglas y respeta el límite diario.
 * Es la implementación de referencia: la puntuación oficial la calcula el servicio Scoring del backend
 * con estas mismas reglas.
 */
class DefaultScoringEngine @Inject constructor(
    private val rules: Set<@JvmSuppressWildcards ScoringRule>,
) : ScoringEngine {

    override fun score(event: DomainEvent, context: ScoringContext): List<ScoreEntry> {
        var remaining = (DAILY_CAP - context.pointsToday.value).coerceAtLeast(0)
        return rules.sortedBy { it.id }.mapNotNull { rule ->
            val points = rule.evaluate(event, context).value.coerceAtMost(remaining)
            if (points <= 0) return@mapNotNull null
            remaining -= points
            ScoreEntry(context.userId, Points(points), rule.id, event.occurredAt)
        }
    }

    companion object {
        const val DAILY_CAP = 60
    }
}
