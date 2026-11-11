package com.caracolas.gestortrainer.feature.nutrition.stub

import com.caracolas.gestortrainer.feature.nutrition.api.NutritionPlanService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NutritionStubModule {
    @Binds
    abstract fun bind(impl: UnavailableNutritionPlanService): NutritionPlanService
}
