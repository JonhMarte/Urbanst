package com.caracolas.gestortrainer.feature.payments.impl

import androidx.compose.foundation.background
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
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequest
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestService
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.launch

/** Fila «Pago pendiente» del perfil del alumno: ve la solicitud del trainer y paga en la pantalla del proveedor. */
@Composable
fun ClientPaymentScreen(
    clientId: ClientId,
    paymentRequestService: PaymentRequestService,
    profileRepository: ProfileRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pending by paymentRequestService.observePendingForClient(clientId).collectAsState(initial = emptyList())

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text("Pago pendiente", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (pending.isEmpty()) {
            Text("No tienes pagos pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        pending.forEach { request -> PendingPaymentCard(request, paymentRequestService, profileRepository) }
    }
}

@Composable
private fun PendingPaymentCard(
    request: PaymentRequest,
    paymentRequestService: PaymentRequestService,
    profileRepository: ProfileRepository,
) {
    var trainerName by remember(request.trainerId) { mutableStateOf(request.trainerId.value) }
    var trainerPublicId by remember(request.trainerId) { mutableStateOf(request.trainerId.value) }
    var result by remember(request.id) { mutableStateOf<String?>(null) }
    var paying by remember(request.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(request.trainerId) {
        profileRepository.observeTrainer(request.trainerId).collect { profile ->
            profile?.let {
                trainerName = it.personal.fullName
                trainerPublicId = it.publicId.value
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(trainerName, fontWeight = FontWeight.Bold)
        Text("ID: $trainerPublicId", style = MaterialTheme.typography.bodySmall)
        Text("${request.period.label()} · ${"%.2f".format(request.amount.amountMinor / 100.0)} €")
        result?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        PrimaryButton(
            text = if (paying) "Abriendo pasarela…" else "Pagar",
            onClick = {
                paying = true
                scope.launch {
                    val payResult = paymentRequestService.pay(request.id)
                    paying = false
                    result = when (payResult) {
                        is PaymentResult.Success -> "Pago completado."
                        is PaymentResult.Cancelled -> "Pago cancelado."
                        is PaymentResult.Failed -> payResult.reason
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
