package com.caracolas.gestortrainer.feature.media.impl

import com.caracolas.gestortrainer.core.model.LocalFile
import com.caracolas.gestortrainer.core.model.MediaKind
import com.caracolas.gestortrainer.core.model.RemoteMedia
import com.caracolas.gestortrainer.feature.media.api.MediaUploader
import java.util.UUID
import javax.inject.Inject

/** Simula la subida a S3 + CloudFront mientras no exista el backend (Fase 1). */
class InMemoryMediaUploader @Inject constructor() : MediaUploader {
    override suspend fun upload(file: LocalFile, kind: MediaKind): Result<RemoteMedia> =
        Result.success(RemoteMedia(id = UUID.randomUUID().toString(), url = file.uri, kind = kind))
}
