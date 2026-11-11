package com.caracolas.gestortrainer.feature.nutrition.stub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.caracolas.gestortrainer.core.designsystem.ComingSoonButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.featureflags.FeatureFlags
import com.caracolas.gestortrainer.core.featureflags.Flag

/** Botón de entrada a Nutrición. Con el flag apagado muestra "Próximamente". */
@Composable
fun NutritionEntryButton(flags: FeatureFlags, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    if (flags.isEnabled(Flag.NUTRITION)) {
        SecondaryButton(text = "Nutrición", onClick = onOpen, modifier = modifier)
    } else {
        ComingSoonButton(label = "Nutrición", modifier = modifier)
    }
}
