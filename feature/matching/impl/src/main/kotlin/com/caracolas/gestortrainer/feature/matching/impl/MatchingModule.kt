package com.caracolas.gestortrainer.feature.matching.impl

import com.caracolas.gestortrainer.feature.matching.api.MatchingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MatchingModule {
    @Binds abstract fun matchingService(impl: InMemoryMatchingService): MatchingService
}
