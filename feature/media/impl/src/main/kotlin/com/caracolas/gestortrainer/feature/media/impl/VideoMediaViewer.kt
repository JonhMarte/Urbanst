package com.caracolas.gestortrainer.feature.media.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.caracolas.gestortrainer.core.model.ExerciseMedia
import com.caracolas.gestortrainer.feature.media.api.ExerciseMediaViewer
import javax.inject.Inject

/** Visor de video del MVP, con Media3/ExoPlayer. */
class VideoMediaViewer @Inject constructor() : ExerciseMediaViewer {
    override fun supports(media: ExerciseMedia): Boolean = media is ExerciseMedia.Video

    @Composable
    override fun Content(media: ExerciseMedia, modifier: Modifier) {
        check(media is ExerciseMedia.Video) { "VideoMediaViewer solo reproduce ExerciseMedia.Video" }
        val context = LocalContext.current

        val player = remember(media.url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(media.url))
                prepare()
            }
        }
        DisposableEffect(player) {
            onDispose { player.release() }
        }

        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    this.player = player
                }
            },
        )
    }
}
