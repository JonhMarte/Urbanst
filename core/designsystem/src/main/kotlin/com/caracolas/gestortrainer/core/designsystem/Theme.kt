package com.caracolas.gestortrainer.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores del diseño del MVP (canvas "GestorTrainer — Pantallas del MVP"). */
object GestorColors {
    val Accent = Color(0xFFC2410C)
    val AccentDark = Color(0xFF9A3412)
    val Ink = Color(0xFF141821)
    val Ground = Color(0xFFF4F5F2)
    val Surface = Color(0xFFFFFFFF)
    val Muted = Color(0xFF5B616B)
    val Line = Color(0xFFE3E5E0)
    val MuscleActive = Color(0xFFD92D20)
    val MuscleIdle = Color(0xFFD5D8D1)
}

private val LightColors = lightColorScheme(
    primary = GestorColors.Accent,
    onPrimary = Color.White,
    secondary = GestorColors.Ink,
    onSecondary = Color.White,
    background = GestorColors.Ground,
    onBackground = GestorColors.Ink,
    surface = GestorColors.Surface,
    onSurface = GestorColors.Ink,
    onSurfaceVariant = GestorColors.Muted,
    outline = GestorColors.Line,
)

@Composable
fun GestorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
