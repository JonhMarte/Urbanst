package com.caracolas.gestortrainer.feature.profile.impl

import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProfileModule {
    @Binds
    abstract fun bindRepository(impl: InMemoryProfileRepository): ProfileRepository

    @Binds
    abstract fun bindValidator(impl: DefaultProfileValidator): ProfileValidator
}
