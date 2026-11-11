package com.caracolas.gestortrainer.feature.onboarding.impl

import com.caracolas.gestortrainer.feature.onboarding.api.OnboardingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnboardingModule {
    @Binds
    abstract fun bindService(impl: DefaultOnboardingService): OnboardingService
}
