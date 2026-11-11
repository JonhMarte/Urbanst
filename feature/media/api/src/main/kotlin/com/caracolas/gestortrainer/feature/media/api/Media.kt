package com.caracolas.gestortrainer.feature.media.api

import com.caracolas.gestortrainer.core.model.ExerciseMedia
import com.caracolas.gestortrainer.core.model.LocalFile
import com.caracolas.gestortrainer.core.model.MediaKind
import com.caracolas.gestortrainer.core.model.RemoteMedia
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * PUNTO DE INTEGRACIÓN DEL 3D.
 * Cada visor declara qué tipo de media sabe mostrar. En el MVP solo existe el visor de video.
 * El futuro Unity3DViewer (integrations/viewer3d) implementará esta interfaz para
 * ExerciseMedia.Model3D y se registrará en MediaViewerRegistry, sin cambiar ninguna pantalla.
 */
interface ExerciseMediaViewer {
    fun supports(media: ExerciseMedia): Boolean

    @Composable
    fun Content(media: ExerciseMedia, modifier: Modifier)
}

/** Elige el visor adecuado para cada media. */
interface MediaViewerRegistry {
    fun viewerFor(media: ExerciseMedia): ExerciseMediaViewer?
}

/** Sube videos del trainer y fotos del alumno. */
interface MediaUploader {
    suspend fun upload(file: LocalFile, kind: MediaKind): Result<RemoteMedia>
}
