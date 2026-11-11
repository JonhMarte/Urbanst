package com.caracolas.gestortrainer.feature.clients.impl

import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.clients.api.TrainerNotesService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ClientsModule {
    @Binds
    abstract fun bindLinkService(impl: DefaultLinkService): LinkService

    @Binds
    abstract fun bindClientRoster(impl: DefaultClientRosterRepository): ClientRosterRepository

    @Binds
    abstract fun bindTrainerNotes(impl: DefaultTrainerNotesService): TrainerNotesService
}
