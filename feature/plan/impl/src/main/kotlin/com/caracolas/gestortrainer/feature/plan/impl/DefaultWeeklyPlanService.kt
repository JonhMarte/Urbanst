package com.caracolas.gestortrainer.feature.plan.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.plan.api.WeeklyPlan
import com.caracolas.gestortrainer.feature.plan.api.WeeklyPlanService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class DefaultWeeklyPlanService @Inject constructor(
    private val assignmentService: AssignmentService,
) : WeeklyPlanService {

    override fun observeWeek(clientId: ClientId, weekStart: LocalDate): Flow<WeeklyPlan> {
        val range = DateRange(weekStart, weekStart.plusDays(6))
        return assignmentService.observeCalendar(clientId, range).map { assignments ->
            val days = (0L..6L).associate { offset ->
                val date = weekStart.plusDays(offset)
                date to assignments.filter { it.date == date }
            }
            WeeklyPlan(clientId, weekStart, days)
        }
    }
}
