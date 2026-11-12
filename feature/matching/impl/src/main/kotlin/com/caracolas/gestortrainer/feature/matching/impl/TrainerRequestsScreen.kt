package com.caracolas.gestortrainer.feature.matching.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.matching.api.MatchingService
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequest
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequestStatus
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.launch

private enum class RequestsTab { NUEVAS, PROPUESTA_ENVIADA }

/** Bandeja del trainer para las solicitudes que llegan desde el descubrimiento. */
@Composable
fun TrainerRequestsScreen(
    trainerId: TrainerId,
    matchingService: MatchingService,
    profileRepository: ProfileRepository,
    onOpenProposalComposer: (TrainerRequest) -> Unit,
    onOpenFullQuestionnaire: (TrainerRequest) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(RequestsTab.NUEVAS) }
    val requests by matchingService.observeForTrainer(trainerId).collectAsState(initial = emptyList())
    val nuevas = requests.filter { it.status == TrainerRequestStatus.NEW }
    val conPropuesta = requests.filter { it.status == TrainerRequestStatus.PROPOSAL_SENT }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text("Solicitudes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabChip("Nuevas · ${nuevas.size}", tab == RequestsTab.NUEVAS) { tab = RequestsTab.NUEVAS }
            TabChip("Propuesta enviada · ${conPropuesta.size}", tab == RequestsTab.PROPUESTA_ENVIADA) { tab = RequestsTab.PROPUESTA_ENVIADA }
        }

        val shown = if (tab == RequestsTab.NUEVAS) nuevas else conPropuesta
        if (shown.isEmpty()) {
            Text("No hay solicitudes aquí.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        shown.forEach { request ->
            RequestCard(
                request = request,
                profileRepository = profileRepository,
                onReject = { matchingService.reject(request.id) },
                onSendProposal = { onOpenProposalComposer(request) },
                onOpenFullQuestionnaire = { onOpenFullQuestionnaire(request) },
            )
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) PrimaryButton(text = label, onClick = onClick) else SecondaryButton(text = label, onClick = onClick)
}

@Composable
private fun RequestCard(
    request: TrainerRequest,
    profileRepository: ProfileRepository,
    onReject: suspend () -> Unit,
    onSendProposal: () -> Unit,
    onOpenFullQuestionnaire: () -> Unit,
) {
    var expanded by remember(request.id) { mutableStateOf(false) }
    var clientName by remember(request.id) { mutableStateOf(request.clientId.value) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(request.clientId) {
        profileRepository.observeClient(request.clientId).collect { profile ->
            profile?.let { clientName = it.personal.fullName }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(enabled = !expanded) { expanded = true }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(clientName, fontWeight = FontWeight.Bold)
            Text(request.status.label(), style = MaterialTheme.typography.bodySmall)
        }
        if (!expanded) {
            Text(request.message, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        } else {
            Text(request.message)
            if (request.goals.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    request.goals.forEach { goal ->
                        Text(
                            goal.label(),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (request.shareFlags.photoShared) {
                Text("Foto compartida (placeholder)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (request.shareFlags.questionnaireShared) {
                SecondaryButton(text = "Ver cuestionario completo", onClick = onOpenFullQuestionnaire)
            }
            if (request.status == TrainerRequestStatus.NEW) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Rechazar", onClick = { scope.launch { onReject() } }, modifier = Modifier.weight(1f))
                    PrimaryButton(text = "Enviar propuesta", onClick = onSendProposal, modifier = Modifier.weight(1f))
                }
            }
            SecondaryButton(text = "Ver menos", onClick = { expanded = false })
        }
    }
}
