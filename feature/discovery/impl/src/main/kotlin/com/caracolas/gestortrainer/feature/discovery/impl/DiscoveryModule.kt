package com.caracolas.gestortrainer.feature.discovery.impl

import com.caracolas.gestortrainer.feature.discovery.api.DiscoveryService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DiscoveryModule {
    @Binds abstract fun discoveryService(impl: DefaultDiscoveryService): DiscoveryService
}
