package com.caracolas.gestortrainer.feature.clients.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.feature.clients.api.LinkError
import com.caracolas.gestortrainer.feature.clients.api.LinkException
import com.caracolas.gestortrainer.feature.clients.api.LinkRequest
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.launch

/** ADD New User, solicitudes pendientes y alumnos activos. */
@Composable
fun ClientsScreen(
    trainerId: TrainerId,
    linkService: LinkService,
    clientRosterRepository: ClientRosterRepository,
    profileRepository: ProfileRepository,
    modifier: Modifier = Modifier,
    onSelectClient: (ClientId) -> Unit = {},
) {
    var clientIdText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val incoming by linkService.observeIncoming(UserId(trainerId.value)).collectAsState(initial = emptyList())
    val activeClients by clientRosterRepository.observeActiveClients(trainerId).collectAsState(initial = emptyList())

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Usuarios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ADD New User", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = clientIdText,
                    onValueChange = { clientIdText = it },
                    placeholder = { Text("ID del alumno (ej. AL-30917)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Agregar",
                    onClick = {
                        val publicId = clientIdText.trim()
                        scope.launch {
                            val result = linkService.addByClientId(trainerId, PublicId(publicId))
                            message = result.fold(
                                onSuccess = { "Invitación enviada." },
                                onFailure = { it.toUserMessage() },
                            )
                            if (result.isSuccess) clientIdText = ""
                        }
                    },
                )
            }
            Text(
                "El alumno recibe la invitación y su tarjeta aparece aquí cuando la acepta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }

        Text("Solicitudes pendientes · ${incoming.size}", fontWeight = FontWeight.Bold)
        incoming.forEach { request ->
            PendingClientRequestCard(
                request = request,
                profileRepository = profileRepository,
                onAccept = { scope.launch { linkService.accept(request.id) } },
                onReject = { scope.launch { linkService.reject(request.id) } },
            )
        }

        Text("Alumnos activos · ${activeClients.size}", fontWeight = FontWeight.Bold)
        activeClients.forEach { card ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onSelectClient(card.clientId) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column {
                    Text(card.displayName, fontWeight = FontWeight.Bold)
                    Text(card.todaySummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PendingClientRequestCard(
    request: LinkRequest,
    profileRepository: ProfileRepository,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    var displayName by remember(request.id) { mutableStateOf(request.clientId.value) }
    LaunchedEffect(request.clientId) {
        profileRepository.observeClient(request.clientId).collect { profile ->
            profile?.let { displayName = it.personal.fullName }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(displayName, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(text = "Rechazar", onClick = onReject, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Aceptar", onClick = onAccept, modifier = Modifier.weight(1f))
        }
    }
}

private fun Throwable.toUserMessage(): String = when ((this as? LinkException)?.error) {
    LinkError.TRAINER_NOT_FOUND -> "No existe ningún trainer con ese ID."
    LinkError.CLIENT_NOT_FOUND -> "No existe ningún alumno con ese ID."
    LinkError.ALREADY_LINKED -> "Ya está vinculado contigo."
    LinkError.REQUEST_ALREADY_PENDING -> "Ya hay una solicitud pendiente con este alumno."
    null -> "No se pudo completar la operación."
}
