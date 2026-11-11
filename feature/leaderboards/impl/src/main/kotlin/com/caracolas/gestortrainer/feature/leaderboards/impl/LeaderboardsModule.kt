package com.caracolas.gestortrainer.feature.leaderboards.impl

import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LeaderboardsModule {
    @Binds abstract fun leaderboardService(impl: DefaultLeaderboardService): LeaderboardService
}
