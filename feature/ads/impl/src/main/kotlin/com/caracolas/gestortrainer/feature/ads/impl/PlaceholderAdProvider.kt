package com.caracolas.gestortrainer.feature.ads.impl

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.GestorColors
import com.caracolas.gestortrainer.feature.ads.api.AdPlacement
import com.caracolas.gestortrainer.feature.ads.api.AdProvider
import javax.inject.Inject

/** Hueco reservado para publicidad. El proveedor real (y la comprobación de membresía) llega en la Fase 3. */
class PlaceholderAdProvider @Inject constructor() : AdProvider {
    @Composable
    override fun Banner(placement: AdPlacement, modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, GestorColors.MuscleIdle, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Publicidad · solo en el plan gratuito", style = MaterialTheme.typography.bodySmall)
        }
    }
}
