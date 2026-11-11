package com.caracolas.gestortrainer.feature.groups.stub

import com.caracolas.gestortrainer.feature.groups.api.GroupClassService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GroupsStubModule {
    @Binds
    abstract fun bind(impl: UnavailableGroupClassService): GroupClassService
}
