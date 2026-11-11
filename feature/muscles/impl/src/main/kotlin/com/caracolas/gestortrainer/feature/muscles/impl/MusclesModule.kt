package com.caracolas.gestortrainer.feature.muscles.impl

import com.caracolas.gestortrainer.feature.muscles.api.MuscleCatalog
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MusclesModule {
    @Binds
    abstract fun bindCatalog(impl: DefaultMuscleCatalog): MuscleCatalog

    @Binds
    abstract fun bindRenderer(impl: SilhouetteMuscleMapRenderer): MuscleMapRenderer
}
