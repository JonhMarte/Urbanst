package com.caracolas.gestortrainer.feature.plan.impl

import com.caracolas.gestortrainer.feature.plan.api.WeeklyPlanService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlanModule {
    @Binds
    abstract fun bindWeeklyPlanService(impl: DefaultWeeklyPlanService): WeeklyPlanService
}
