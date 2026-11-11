package com.caracolas.gestortrainer.feature.plan.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.feature.calendar.api.Assignment
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class WeeklyPlan(
    val clientId: ClientId,
    val weekStart: LocalDate,
    val days: Map<LocalDate, List<Assignment>>,
)

/** Lo que ve el alumno en "Mi plan". */
interface WeeklyPlanService {
    fun observeWeek(clientId: ClientId, weekStart: LocalDate): Flow<WeeklyPlan>
}
