package com.caracolas.gestortrainer.core.model

/**
 * Contenido visual de un ejercicio.
 * MVP: solo [Video]. [Model3D] queda reservado para el visor 3D/AR futuro
 * (integrations/viewer3d); hoy ningún visor lo acepta.
 */
sealed interface ExerciseMedia {
    data class Video(val url: String, val durationMs: Long) : ExerciseMedia
    data class Model3D(val assetId: String) : ExerciseMedia
}

enum class MediaKind { EXERCISE_VIDEO, ACTIVITY_PHOTO, PROFILE_PHOTO }

/** Archivo local antes de subirlo (ruta o content:// del dispositivo). */
data class LocalFile(val uri: String, val mimeType: String)

/** Archivo ya subido al almacenamiento (S3 + CloudFront). */
data class RemoteMedia(val id: String, val url: String, val kind: MediaKind)
