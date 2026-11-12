package com.caracolas.gestortrainer.feature.discovery.impl

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
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.clients.api.LinkError
import com.caracolas.gestortrainer.feature.clients.api.LinkException
import com.caracolas.gestortrainer.feature.clients.api.LinkRequest
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.discovery.api.DiscoveryService
import com.caracolas.gestortrainer.feature.discovery.api.TrainerQuery
import com.caracolas.gestortrainer.feature.discovery.api.TrainerSummary
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.launch

/**
 * Buscar trainer por ID o por disciplina, enviar solicitud, y responder a las invitaciones que manda un trainer.
 * [initialDiscipline] llega desde el carrusel de categorías de ClientHomeV2, ya filtrado.
 * [extraActions] lo rellena :app: así este módulo no depende de otras implementaciones.
 */
@Composable
fun DiscoveryScreen(
    clientId: ClientId,
    linkService: LinkService,
    profileRepository: ProfileRepository,
    discoveryService: DiscoveryService,
    initialDiscipline: String? = null,
    modifier: Modifier = Modifier,
    onOpenProfile: (TrainerSummary) -> Unit = {},
    extraActions: @Composable () -> Unit = {},
) {
    var trainerIdText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var disciplineText by remember { mutableStateOf(initialDiscipline.orEmpty()) }
    val scope = rememberCoroutineScope()

    val incoming by linkService.observeIncoming(UserId(clientId.value)).collectAsState(initial = emptyList())
    val searchResults by discoveryService.searchTrainers(TrainerQuery(discipline = disciplineText.ifBlank { null }))
        .collectAsState(initial = emptyList())

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Encuentra tu trainer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Buscar por disciplina", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = disciplineText,
                onValueChange = { disciplineText = it },
                placeholder = { Text("Ej. Yoga, Running…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (disciplineText.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    Text("Sin resultados todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                searchResults.forEach { summary ->
                    TrainerSummaryCard(
                        summary = summary,
                        onOpenProfile = { onOpenProfile(summary) },
                        onRequest = {
                            scope.launch {
                                val result = linkService.requestByTrainerId(clientId, summary.publicId)
                                message = result.fold(
                                    onSuccess = { "Solicitud enviada a ${summary.displayName}." },
                                    onFailure = { it.toUserMessage() },
                                )
                            }
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("¿Ya tienes su ID?", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = trainerIdText,
                    onValueChange = { trainerIdText = it },
                    placeholder = { Text("ID del trainer (ej. TR-48213)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Enviar solicitud",
                    onClick = {
                        val publicId = trainerIdText.trim()
                        scope.launch {
                            val result = linkService.requestByTrainerId(clientId, PublicId(publicId))
                            message = result.fold(
                                onSuccess = { "Solicitud enviada." },
                                onFailure = { it.toUserMessage() },
                            )
                            if (result.isSuccess) trainerIdText = ""
                        }
                    },
                )
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }

        if (incoming.isNotEmpty()) {
            Text("Solicitudes pendientes · ${incoming.size}", fontWeight = FontWeight.Bold)
            incoming.forEach { request ->
                PendingTrainerRequestCard(
                    request = request,
                    profileRepository = profileRepository,
                    onAccept = { scope.launch { linkService.accept(request.id) } },
                    onReject = { scope.launch { linkService.reject(request.id) } },
                )
            }
        }

        extraActions()
    }
}

@Composable
private fun PendingTrainerRequestCard(
    request: LinkRequest,
    profileRepository: ProfileRepository,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    var displayName by remember(request.id) { mutableStateOf(request.trainerId.value) }
    LaunchedEffect(request.trainerId) {
        profileRepository.observeTrainer(request.trainerId).collect { profile ->
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

@Composable
private fun TrainerSummaryCard(summary: TrainerSummary, onOpenProfile: () -> Unit, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenProfile)) {
            Text(summary.displayName, fontWeight = FontWeight.Bold)
            Text(
                "${summary.mainDiscipline} · ${"%.1f".format(summary.averageStars)} ★ · ${summary.yearsAsTrainer} años",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryButton(text = "Enviar solicitud", onClick = onRequest)
    }
}

private fun Throwable.toUserMessage(): String = when ((this as? LinkException)?.error) {
    LinkError.TRAINER_NOT_FOUND -> "No existe ningún trainer con ese ID."
    LinkError.CLIENT_NOT_FOUND -> "No existe ningún alumno con ese ID."
    LinkError.ALREADY_LINKED -> "Ya estás vinculado con ese trainer."
    LinkError.REQUEST_ALREADY_PENDING -> "Ya enviaste una solicitud a ese trainer."
    null -> "No se pudo completar la operación."
}
