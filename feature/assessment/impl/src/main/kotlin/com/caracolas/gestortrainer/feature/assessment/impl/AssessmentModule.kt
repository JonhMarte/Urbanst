package com.caracolas.gestortrainer.feature.assessment.impl

import com.caracolas.gestortrainer.feature.assessment.api.AssessmentRepository
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AssessmentModule {
    @Binds abstract fun repository(impl: InMemoryAssessmentRepository): AssessmentRepository
    @Binds abstract fun validator(impl: DefaultAssessmentValidator): AssessmentValidator
}
