package com.caracolas.gestortrainer.feature.groups.stub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.caracolas.gestortrainer.core.designsystem.ComingSoonButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.featureflags.FeatureFlags
import com.caracolas.gestortrainer.core.featureflags.Flag

/** Botón de entrada a Clases grupales. Con el flag apagado muestra "Próximamente". */
@Composable
fun GroupsEntryButton(flags: FeatureFlags, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    if (flags.isEnabled(Flag.GROUP_CLASSES)) {
        SecondaryButton(text = "Clases grupales", onClick = onOpen, modifier = modifier)
    } else {
        ComingSoonButton(label = "Clases grupales", modifier = modifier)
    }
}
