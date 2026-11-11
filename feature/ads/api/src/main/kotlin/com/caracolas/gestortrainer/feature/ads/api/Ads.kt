package com.caracolas.gestortrainer.feature.ads.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class AdPlacement { CLIENT_HOME_BOTTOM }

/** Publicidad: solo para alumnos sin membresía, nunca en pantallas del trainer. */
interface AdProvider {
    @Composable
    fun Banner(placement: AdPlacement, modifier: Modifier)
}
