package com.caracolas.gestortrainer.feature.matching.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.feature.matching.api.MatchingService
import com.caracolas.gestortrainer.feature.matching.api.RequestGoal
import com.caracolas.gestortrainer.feature.matching.api.ShareFlags
import kotlinx.coroutines.launch

/** Formulario de solicitud al trainer encontrado en el descubrimiento (carrusel / búsqueda). */
@Composable
fun ClientRequestScreen(
    clientId: ClientId,
    trainerPublicId: PublicId,
    trainerDisplayName: String,
    matchingService: MatchingService,
    onSent: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var message by remember { mutableStateOf("") }
    var goals by remember { mutableStateOf(setOf<RequestGoal>()) }
    var questionnaireShared by remember { mutableStateOf(false) }
    var photoShared by remember { mutableStateOf(false) }
    var bodyDataShared by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text("Solicitud a $trainerDisplayName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("¿Qué buscas?") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Objetivo", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RequestGoal.entries.forEach { goal ->
                val selected = goal in goals
                val toggle = { goals = if (selected) goals - goal else goals + goal }
                if (selected) {
                    PrimaryButton(text = goal.label(), onClick = toggle)
                } else {
                    SecondaryButton(text = goal.label(), onClick = toggle)
                }
            }
        }

        Text("Qué compartes con este trainer", fontWeight = FontWeight.Bold)
        Text(
            "Podrás revocarlo luego desde tu perfil.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ShareToggleRow("Mi cuestionario", questionnaireShared) { questionnaireShared = it }
        ShareToggleRow("Mi foto", photoShared) { photoShared = it }
        ShareToggleRow("Peso, altura y edad", bodyDataShared) { bodyDataShared = it }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        PrimaryButton(
            text = if (sending) "Enviando…" else "Enviar solicitud",
            onClick = {
                if (message.isBlank()) {
                    error = "Cuéntale al trainer qué buscas."
                } else {
                    sending = true
                    scope.launch {
                        val result = matchingService.sendRequest(
                            clientId = clientId,
                            trainerPublicId = trainerPublicId,
                            message = message,
                            goals = goals.toList(),
                            shareFlags = ShareFlags(questionnaireShared, photoShared, bodyDataShared),
                        )
                        sending = false
                        result.fold(onSuccess = { onSent() }, onFailure = { error = "No se pudo enviar la solicitud." })
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ShareToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
