package com.caracolas.gestortrainer.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ButtonShape = RoundedCornerShape(14.dp)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        modifier = modifier.heightIn(min = 52.dp),
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = GestorColors.Ink),
        modifier = modifier.heightIn(min = 52.dp),
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

/** Botón desactivado para funciones reservadas (nutrición, clases grupales…). */
@Composable
fun ComingSoonButton(label: String, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {},
        enabled = false,
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, GestorColors.MuscleIdle),
        modifier = modifier.heightIn(min = 48.dp),
    ) { Text("$label · Próximamente") }
}

/** Pantalla provisional mientras se construye una funcionalidad. */
@Composable
fun PlaceholderScreen(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        content()
    }
}
