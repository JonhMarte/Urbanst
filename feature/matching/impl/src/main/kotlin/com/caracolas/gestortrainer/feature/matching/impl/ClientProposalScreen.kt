package com.caracolas.gestortrainer.feature.matching.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.caracolas.gestortrainer.core.model.TrainerRequestId
import com.caracolas.gestortrainer.feature.matching.api.MatchingService
import com.caracolas.gestortrainer.feature.matching.api.Proposal
import com.caracolas.gestortrainer.feature.matching.api.ProposalPayment
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequest
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequestStatus
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private val UNLOCKS = listOf("Chat con tu trainer", "Plan semanal de actividades", "Puntos y clasificaciones")

/** Lista de propuestas que el alumno aún no ha respondido, para la fila «Propuestas» del perfil. */
@Composable
fun ClientProposalsListScreen(
    clientId: ClientId,
    matchingService: MatchingService,
    profileRepository: ProfileRepository,
    onOpenProposal: (TrainerRequest) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val proposals by matchingService.observeForClient(clientId).collectAsState(initial = emptyList())
    val pending = proposals.filter { it.status == TrainerRequestStatus.PROPOSAL_SENT }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text("Propuestas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (pending.isEmpty()) {
            Text("No tienes propuestas pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        pending.forEach { request -> ProposalSummaryRow(request, profileRepository) { onOpenProposal(request) } }
    }
}

@Composable
private fun ProposalSummaryRow(request: TrainerRequest, profileRepository: ProfileRepository, onClick: () -> Unit) {
    var trainerName by remember(request.trainerId) { mutableStateOf(request.trainerId.value) }
    LaunchedEffect(request.trainerId) {
        profileRepository.observeTrainer(request.trainerId).collect { profile ->
            profile?.let { trainerName = it.personal.fullName }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(trainerName, fontWeight = FontWeight.Bold)
        Text(request.proposal?.title.orEmpty(), style = MaterialTheme.typography.bodySmall)
    }
}

/** El alumno ve la propuesta del trainer y, si acepta, paga para desbloquear el vínculo. */
@Composable
fun ClientProposalScreen(
    request: TrainerRequest,
    matchingService: MatchingService,
    profileRepository: ProfileRepository,
    onAccepted: () -> Unit,
    onRejected: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val proposal = request.proposal
    var trainerName by remember(request.trainerId) { mutableStateOf(request.trainerId.value) }
    var showPaymentSheet by remember { mutableStateOf(false) }

    LaunchedEffect(request.trainerId) {
        profileRepository.observeTrainer(request.trainerId).collect { profile ->
            profile?.let { trainerName = it.personal.fullName }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)

        if (proposal == null) {
            Text("Esta solicitud todavía no tiene propuesta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        Text(trainerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(proposal.title, fontWeight = FontWeight.Bold)
        Text(proposal.description)
        Text("Duración: ${proposal.durationLabel}", style = MaterialTheme.typography.bodySmall)

        val daysLeft = Duration.between(Instant.now(), proposal.expiresAt).toDays().coerceAtLeast(0)
        Text(
            if (daysLeft <= 0) "Esta oferta ha caducado." else "Caduca en $daysLeft días",
            color = if (daysLeft <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        val amountLabel = when (val payment = proposal.payment) {
            is ProposalPayment.OneOff -> "${"%.2f".format(payment.amount.amountMinor / 100.0)} € (pago único)"
            is ProposalPayment.Recurring -> "${"%.2f".format(payment.amount.amountMinor / 100.0)} € / ${payment.period.matchingLabel().lowercase()}"
        }
        Text("Total: $amountLabel", fontWeight = FontWeight.Bold)

        Text("Qué se desbloquea", fontWeight = FontWeight.Bold)
        UNLOCKS.forEach { Text("· $it") }

        if (!showPaymentSheet) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "Rechazar",
                    onClick = onRejected,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Aceptar y pagar",
                    onClick = { showPaymentSheet = true },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            PaymentSheet(
                requestId = request.id,
                matchingService = matchingService,
                onAccepted = onAccepted,
            )
        }
    }
}

@Composable
private fun PaymentSheet(
    requestId: TrainerRequestId,
    matchingService: MatchingService,
    onAccepted: () -> Unit,
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var paying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun pay() {
        paying = true
        scope.launch {
            val result = matchingService.acceptAndPay(requestId)
            paying = false
            when (result) {
                is PaymentResult.Success -> onAccepted()
                is PaymentResult.Cancelled -> error = "Pago cancelado."
                is PaymentResult.Failed -> error = result.reason
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Pago procesado por Stripe. GestorTrainer no guarda los datos de tu tarjeta.", style = MaterialTheme.typography.bodySmall)

        PrimaryButton(text = if (paying) "Procesando…" else "Pagar con Google Pay", onClick = { pay() }, modifier = Modifier.fillMaxWidth())

        Text("o con tarjeta", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(cardNumber, { cardNumber = it }, label = { Text("Número de tarjeta") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(expiry, { expiry = it }, label = { Text("Caducidad") }, modifier = Modifier.weight(1f))
            OutlinedTextField(cvc, { cvc = it }, label = { Text("CVC") }, modifier = Modifier.weight(1f))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        SecondaryButton(text = if (paying) "Procesando…" else "Pagar con tarjeta", onClick = { pay() }, modifier = Modifier.fillMaxWidth())
    }
}
