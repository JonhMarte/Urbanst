package com.caracolas.gestortrainer.feature.tracking.impl

import com.caracolas.gestortrainer.feature.tracking.api.ActivityTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TrackingModule {
    @Binds abstract fun activityTracker(impl: InMemoryActivityTracker): ActivityTracker
}
