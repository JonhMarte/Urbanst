package com.caracolas.gestortrainer.feature.calendar.impl

import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CalendarModule {
    @Binds
    abstract fun bindAssignmentService(impl: InMemoryAssignmentService): AssignmentService
}
