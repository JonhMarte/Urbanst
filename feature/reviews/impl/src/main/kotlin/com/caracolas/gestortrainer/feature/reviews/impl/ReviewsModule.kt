package com.caracolas.gestortrainer.feature.reviews.impl

import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReviewsModule {
    @Binds abstract fun reviewService(impl: InMemoryReviewService): ReviewService
}
