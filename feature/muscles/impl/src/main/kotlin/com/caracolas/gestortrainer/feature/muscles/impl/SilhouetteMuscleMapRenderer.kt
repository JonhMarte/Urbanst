package com.caracolas.gestortrainer.feature.muscles.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.caracolas.gestortrainer.core.designsystem.GestorColors
import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import javax.inject.Inject

/** Silueta 2D frontal (lienzo de 96 x 176 escalado). Los isquiotibiales se marcan sobre los muslos. */
class SilhouetteMuscleMapRenderer @Inject constructor() : MuscleMapRenderer {

    private data class Part(val muscle: String?, val x: Float, val y: Float, val w: Float, val h: Float)

    private val parts = listOf(
        Part("shoulders", 30f, 30f, 36f, 12f),
        Part("chest", 33f, 42f, 30f, 16f),
        Part("core", 35f, 59f, 26f, 22f),
        Part("arms", 19f, 34f, 10f, 30f),
        Part("arms", 67f, 34f, 10f, 30f),
        Part(null, 17f, 66f, 9f, 24f),
        Part(null, 70f, 66f, 9f, 24f),
        Part("glutes", 34f, 82f, 28f, 12f),
        Part("quads", 34f, 95f, 13f, 36f),
        Part("quads", 49f, 95f, 13f, 36f),
        Part("calves", 35f, 133f, 11f, 34f),
        Part("calves", 50f, 133f, 11f, 34f),
    )

    @Composable
    override fun Render(selected: Set<MuscleId>, modifier: Modifier) {
        val ids = selected.map { it.value }.toSet()
        val legsActive = "hamstrings" in ids
        Canvas(modifier.semantics { contentDescription = "Silueta con los músculos seleccionados en rojo" }) {
            val sx = size.width / 96f
            val sy = size.height / 176f
            drawCircle(GestorColors.MuscleIdle, radius = 10f * minOf(sx, sy), center = Offset(48f * sx, 16f * sy))
            parts.forEach { p ->
                val active = p.muscle != null && (p.muscle in ids || (legsActive && p.muscle == "quads"))
                drawPart(p, sx, sy, if (active) GestorColors.MuscleActive else GestorColors.MuscleIdle)
            }
        }
    }

    @Composable
    override fun RenderSelectable(selected: Set<MuscleId>, onToggle: (MuscleId) -> Unit, modifier: Modifier) {
        val ids = selected.map { it.value }.toSet()
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }
        Canvas(
            modifier
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (canvasSize.width == 0 || canvasSize.height == 0) return@detectTapGestures
                        val sx = canvasSize.width / 96f
                        val sy = canvasSize.height / 176f
                        parts.firstOrNull { p ->
                            p.muscle != null &&
                                offset.x in (p.x * sx)..((p.x + p.w) * sx) &&
                                offset.y in (p.y * sy)..((p.y + p.h) * sy)
                        }?.muscle?.let { onToggle(MuscleId(it)) }
                    }
                }
                .semantics { contentDescription = "Silueta: toca una zona para marcarla o desmarcarla" },
        ) {
            val sx = size.width / 96f
            val sy = size.height / 176f
            drawCircle(GestorColors.MuscleIdle, radius = 10f * minOf(sx, sy), center = Offset(48f * sx, 16f * sy))
            parts.forEach { p ->
                val active = p.muscle != null && p.muscle in ids
                drawPart(p, sx, sy, if (active) GestorColors.MuscleActive else GestorColors.MuscleIdle)
            }
        }
    }

    private fun DrawScope.drawPart(p: Part, sx: Float, sy: Float, color: Color) {
        drawRoundRect(
            color = color,
            topLeft = Offset(p.x * sx, p.y * sy),
            size = Size(p.w * sx, p.h * sy),
            cornerRadius = CornerRadius(5f * sx, 5f * sy),
        )
    }
}
