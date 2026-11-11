package com.caracolas.gestortrainer.feature.nutrition.stub

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.feature.nutrition.api.NutritionPlan
import com.caracolas.gestortrainer.feature.nutrition.api.NutritionPlanService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Implementación vacía mientras Flag.NUTRITION esté apagado. */
class UnavailableNutritionPlanService @Inject constructor() : NutritionPlanService {
    override fun observePlan(clientId: ClientId): Flow<NutritionPlan?> = flowOf(null)
}
