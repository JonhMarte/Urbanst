package com.caracolas.gestortrainer.feature.media.impl

import com.caracolas.gestortrainer.feature.media.api.ExerciseMediaViewer
import com.caracolas.gestortrainer.feature.media.api.MediaUploader
import com.caracolas.gestortrainer.feature.media.api.MediaViewerRegistry
import dagger.Binds
import dagger.multibindings.IntoSet
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MediaModule {
    @Binds
    @IntoSet
    abstract fun bindVideoViewer(impl: VideoMediaViewer): ExerciseMediaViewer

    @Binds
    abstract fun bindRegistry(impl: DefaultMediaViewerRegistry): MediaViewerRegistry

    @Binds
    abstract fun bindUploader(impl: InMemoryMediaUploader): MediaUploader
}
