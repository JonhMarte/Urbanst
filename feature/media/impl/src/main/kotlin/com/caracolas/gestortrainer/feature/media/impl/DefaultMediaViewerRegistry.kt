package com.caracolas.gestortrainer.feature.media.impl

import com.caracolas.gestortrainer.core.model.ExerciseMedia
import com.caracolas.gestortrainer.feature.media.api.ExerciseMediaViewer
import com.caracolas.gestortrainer.feature.media.api.MediaViewerRegistry
import javax.inject.Inject

/**
 * Recibe todos los visores registrados con @IntoSet.
 * Para añadir el 3D basta con que integrations/viewer3d aporte otro visor al conjunto.
 */
class DefaultMediaViewerRegistry @Inject constructor(
    private val viewers: Set<@JvmSuppressWildcards ExerciseMediaViewer>,
) : MediaViewerRegistry {
    override fun viewerFor(media: ExerciseMedia): ExerciseMediaViewer? = viewers.firstOrNull { it.supports(media) }
}
