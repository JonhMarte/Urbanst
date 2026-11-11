package com.caracolas.gestortrainer.feature.nutrition.api

import com.caracolas.gestortrainer.core.model.ClientId
import kotlinx.coroutines.flow.Flow

/** RESERVADO. Flag.NUTRITION apagado en el MVP: la UI solo muestra "Próximamente". */
data class NutritionPlan(val clientId: ClientId, val items: List<String>)

interface NutritionPlanService {
    fun observePlan(clientId: ClientId): Flow<NutritionPlan?>
}
