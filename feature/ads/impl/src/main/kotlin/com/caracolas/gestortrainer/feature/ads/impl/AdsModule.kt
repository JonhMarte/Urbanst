package com.caracolas.gestortrainer.feature.ads.impl

import com.caracolas.gestortrainer.feature.ads.api.AdProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AdsModule {
    @Binds
    abstract fun bindAdProvider(impl: PlaceholderAdProvider): AdProvider
}
