package com.caracolas.gestortrainer.feature.payments.impl

import androidx.compose.foundation.background
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
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.payments.api.BillingPeriod
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequest
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestService
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestStatus
import com.caracolas.gestortrainer.feature.payments.api.PayoutProviderRegistry
import com.caracolas.gestortrainer.feature.payments.api.PayoutStatus
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun BillingPeriod.label() = when (this) {
    BillingPeriod.DAILY -> "Diario"
    BillingPeriod.MONTHLY -> "Mensual"
    BillingPeriod.SEMIANNUAL -> "Semestral"
    BillingPeriod.ANNUAL -> "Anual"
}

private fun PaymentRequestStatus.label() = when (this) {
    PaymentRequestStatus.DRAFT -> "Borrador"
    PaymentRequestStatus.SENT -> "Enviada"
    PaymentRequestStatus.PAID -> "Pagada"
    PaymentRequestStatus.CANCELLED -> "Cancelada"
    PaymentRequestStatus.EXPIRED -> "Caducada"
}

private val LINKED_PROVIDERS = listOf(GatewayId.PAYPAL, GatewayId.STRIPE, GatewayId.MERCADO_PAGO)

/** Habilitación de pagos del trainer: método de cobro y solicitudes de pago a sus alumnos. */
@Composable
fun TrainerPaymentsScreen(
    trainerId: TrainerId,
    paymentRequestService: PaymentRequestService,
    payoutProviderRegistry: PayoutProviderRegistry,
    linkService: LinkService,
    profileRepository: ProfileRepository,
    onOpenPayoutLink: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkedAccount by remember {
        combine(LINKED_PROVIDERS.map { payoutProviderRegistry.providerFor(it).observeAccount(trainerId) }) { accounts ->
            accounts.filterNotNull().firstOrNull()
        }
    }.collectAsState(initial = null)

    var period by remember { mutableStateOf(BillingPeriod.MONTHLY) }
    var amountText by remember { mutableStateOf("") }
    var clientIdText by remember { mutableStateOf("") }
    var resolvedClientName by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val requests by paymentRequestService.observeForTrainer(trainerId).collectAsState(initial = emptyList())

    LaunchedEffect(clientIdText) {
        val publicId = clientIdText.trim()
        val client = if (publicId.isBlank()) null else profileRepository.findClientByPublicId(PublicId(publicId))
        val linked = client != null && trainerId in linkService.observeLinkedTrainers(ClientId(client.userId.value)).first()
        resolvedClientName = if (client != null && linked) client.personal.fullName else null
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text("Habilitación de pagos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Método de cobro", fontWeight = FontWeight.Bold)
            val account = linkedAccount
            if (account == null) {
                PrimaryButton(text = "Vincular cuenta de cobro", onClick = onOpenPayoutLink, modifier = Modifier.fillMaxWidth())
            } else {
                Text("${account.provider.providerLabel()} · ${if (account.status == PayoutStatus.ACTIVE) "Activa" else account.status.name}")
                SecondaryButton(text = "Cambiar", onClick = onOpenPayoutLink)
            }
        }

        Text("Nueva solicitud de pago", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BillingPeriod.entries.forEach { option ->
                if (period == option) {
                    PrimaryButton(text = option.label(), onClick = { period = option })
                } else {
                    SecondaryButton(text = option.label(), onClick = { period = option })
                }
            }
        }
        OutlinedTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Monto (EUR)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(clientIdText, { clientIdText = it }, label = { Text("ID del alumno (ej. AL-30917)") }, modifier = Modifier.fillMaxWidth())
        resolvedClientName?.let { Text("Alumno: $it", color = MaterialTheme.colorScheme.primary) }
        if (clientIdText.isNotBlank() && resolvedClientName == null) {
            Text("No es un alumno vinculado contigo.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (linkedAccount == null) {
            Text("Vincula un método de cobro antes de enviar el pago.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "Guardar",
                onClick = {
                    val amountMinor = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    scope.launch {
                        val result = paymentRequestService.saveDraft(trainerId, PublicId(clientIdText.trim()), period, Money(amountMinor, "EUR"))
                        message = result.fold({ "Guardado como borrador." }, { "No se pudo guardar." })
                    }
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "Enviar",
                onClick = {
                    val amountMinor = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    scope.launch {
                        val draft = paymentRequestService.saveDraft(trainerId, PublicId(clientIdText.trim()), period, Money(amountMinor, "EUR"))
                        val sendResult = draft.mapCatching { paymentRequestService.send(it).getOrThrow() }
                        message = sendResult.fold({ "Solicitud enviada." }, { "No se pudo enviar." })
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        if (requests.isNotEmpty()) {
            Text("Solicitudes", fontWeight = FontWeight.Bold)
            requests.sortedByDescending { it.createdAt }.forEach { PaymentRequestRow(it) }
        }
    }
}

@Composable
private fun PaymentRequestRow(request: PaymentRequest) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${request.period.label()} · ${"%.2f".format(request.amount.amountMinor / 100.0)} €")
        Text(request.status.label(), fontWeight = FontWeight.Bold)
    }
}
