package com.caracolas.gestortrainer.feature.activities.impl

import com.caracolas.gestortrainer.feature.activities.api.ActivityValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ValidatorBindings {
    @Binds
    abstract fun bindValidator(impl: DefaultActivityValidator): ActivityValidator
}
