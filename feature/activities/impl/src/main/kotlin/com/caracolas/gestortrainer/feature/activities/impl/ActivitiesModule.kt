package com.caracolas.gestortrainer.feature.activities.impl

import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ActivitiesModule {
    @Binds
    abstract fun bindRepository(impl: InMemoryActivityRepository): ActivityRepository
}
