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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.feature.matching.api.MatchingService
import com.caracolas.gestortrainer.feature.matching.api.Proposal
import com.caracolas.gestortrainer.feature.matching.api.ProposalPayment
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequest
import com.caracolas.gestortrainer.feature.payments.api.BillingPeriod
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

private enum class PaymentKind { UNICO, RECURRENTE }
private val EXPIRY_OPTIONS = listOf(3L, 7L, 14L)

/** El trainer compone la propuesta (título, descripción, duración, precio, caducidad) para una solicitud nueva. */
@Composable
fun ProposalComposerScreen(
    request: TrainerRequest,
    matchingService: MatchingService,
    onSent: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationLabel by remember { mutableStateOf("") }
    var paymentKind by remember { mutableStateOf(PaymentKind.RECURRENTE) }
    var period by remember { mutableStateOf(BillingPeriod.MONTHLY) }
    var amountText by remember { mutableStateOf("") }
    var expiresInDays by remember { mutableStateOf(7L) }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text("Enviar propuesta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Descripción") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(durationLabel, { durationLabel = it }, label = { Text("Duración (ej. 3 meses, 12 sesiones)") }, modifier = Modifier.fillMaxWidth())

        Text("Tipo de pago", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (paymentKind == PaymentKind.UNICO) {
                PrimaryButton(text = "Único (no renueva)", onClick = { paymentKind = PaymentKind.UNICO })
            } else {
                SecondaryButton(text = "Único (no renueva)", onClick = { paymentKind = PaymentKind.UNICO })
            }
            if (paymentKind == PaymentKind.RECURRENTE) {
                PrimaryButton(text = "Periódico", onClick = { paymentKind = PaymentKind.RECURRENTE })
            } else {
                SecondaryButton(text = "Periódico", onClick = { paymentKind = PaymentKind.RECURRENTE })
            }
        }
        if (paymentKind == PaymentKind.RECURRENTE) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BillingPeriod.entries.forEach { option ->
                    if (period == option) {
                        PrimaryButton(text = option.matchingLabel(), onClick = { period = option })
                    } else {
                        SecondaryButton(text = option.matchingLabel(), onClick = { period = option })
                    }
                }
            }
        }
        OutlinedTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Monto (EUR)") }, modifier = Modifier.fillMaxWidth())

        Text("La oferta caduca en", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EXPIRY_OPTIONS.forEach { days ->
                if (expiresInDays == days) {
                    PrimaryButton(text = "$days días", onClick = { expiresInDays = days })
                } else {
                    SecondaryButton(text = "$days días", onClick = { expiresInDays = days })
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        PrimaryButton(
            text = if (sending) "Enviando…" else "Enviar propuesta",
            onClick = {
                val amountMinor = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                if (title.isBlank() || amountMinor <= 0) {
                    error = "Completa el título y un monto válido."
                } else {
                    val money = Money(amountMinor, "EUR")
                    val payment = if (paymentKind == PaymentKind.UNICO) {
                        ProposalPayment.OneOff(money)
                    } else {
                        ProposalPayment.Recurring(period, money)
                    }
                    val proposal = Proposal(
                        title = title,
                        description = description,
                        durationLabel = durationLabel,
                        payment = payment,
                        expiresAt = Instant.now().plus(expiresInDays, ChronoUnit.DAYS),
                    )
                    sending = true
                    scope.launch {
                        val result = matchingService.sendProposal(request.id, proposal)
                        sending = false
                        result.fold(onSuccess = { onSent() }, onFailure = { error = "No se pudo enviar la propuesta." })
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
