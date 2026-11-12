package com.caracolas.gestortrainer.feature.messaging.impl

import com.caracolas.gestortrainer.feature.messaging.api.MessagingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MessagingModule {
    @Binds abstract fun messagingService(impl: InMemoryMessagingService): MessagingService
}
