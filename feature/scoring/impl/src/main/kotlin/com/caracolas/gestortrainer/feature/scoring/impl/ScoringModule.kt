package com.caracolas.gestortrainer.feature.scoring.impl

import com.caracolas.gestortrainer.feature.scoring.api.ScoreRepository
import com.caracolas.gestortrainer.feature.scoring.api.ScoringEngine
import com.caracolas.gestortrainer.feature.scoring.api.ScoringRule
import com.caracolas.gestortrainer.feature.scoring.api.TrainerRankingPolicy
import dagger.Binds
import dagger.multibindings.IntoSet
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ScoringModule {
    @Binds @IntoSet abstract fun timeWindow(rule: TimeWindowRule): ScoringRule
    @Binds @IntoSet abstract fun photo(rule: PhotoEvidenceRule): ScoringRule
    @Binds @IntoSet abstract fun daily(rule: DailyCompletionRule): ScoringRule
    @Binds @IntoSet abstract fun streak(rule: WeeklyStreakRule): ScoringRule

    @Binds abstract fun engine(impl: DefaultScoringEngine): ScoringEngine
    @Binds abstract fun trainerPolicy(impl: BayesianTrainerRankingPolicy): TrainerRankingPolicy
    @Binds abstract fun scoreRepository(impl: InMemoryScoreRepository): ScoreRepository
}
