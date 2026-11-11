package com.caracolas.gestortrainer.feature.media.impl

import com.caracolas.gestortrainer.core.model.ExerciseMedia
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMediaViewerRegistryTest {
    private val registry = DefaultMediaViewerRegistry(setOf(VideoMediaViewer()))

    @Test
    fun elVideoTieneVisor() {
        assertTrue(registry.viewerFor(ExerciseMedia.Video("https://x/v.m3u8", 1000)) is VideoMediaViewer)
    }

    @Test
    fun el3DNoTieneVisorEnElMvp() {
        assertNull(registry.viewerFor(ExerciseMedia.Model3D("sentadilla")))
    }
}
