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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PayoutProviderRegistry
import com.caracolas.gestortrainer.feature.payments.api.PayoutStatus
import kotlinx.coroutines.launch

internal fun GatewayId.providerLabel() = when (this) {
    GatewayId.PAYPAL -> "PayPal"
    GatewayId.STRIPE -> "Cuenta bancaria (Stripe Connect)"
    GatewayId.MERCADO_PAGO -> "Mercado Pago"
    GatewayId.PLAY_BILLING -> "Google Play"
}

/** PayPal, cuenta bancaria (Stripe Connect) o Mercado Pago. Comisión de la plataforma: pendiente de definir. */
private val LINKABLE_PROVIDERS = listOf(GatewayId.PAYPAL, GatewayId.STRIPE, GatewayId.MERCADO_PAGO)

/** Elegir proveedor de cobro, ver requisitos y país, y continuar a la página segura del proveedor. */
@Composable
fun PayoutLinkScreen(
    trainerId: TrainerId,
    payoutProviderRegistry: PayoutProviderRegistry,
    onContinue: (url: String, provider: GatewayId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<GatewayId?>(null) }
    var country by remember { mutableStateOf("España") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text("Vincular cuenta de cobro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        LINKABLE_PROVIDERS.forEach { provider ->
            val isSelected = selected == provider
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(14.dp),
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    provider.providerLabel(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(text = if (isSelected) "Seleccionado" else "Elegir", onClick = { selected = provider })
                if (isSelected) {
                    Text("Necesitas: una cuenta de ${provider.providerLabel()} verificada.", style = MaterialTheme.typography.bodySmall)
                    Text("Recibes: el monto de cada solicitud pagada, menos la comisión de la plataforma.", style = MaterialTheme.typography.bodySmall)
                    Text("Comisiones: pendiente de definir.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        OutlinedTextField(country, { country = it }, label = { Text("País de la cuenta") }, modifier = Modifier.fillMaxWidth())

        Text(
            "Se abrirá la página segura del proveedor. GestorTrainer nunca ve tu contraseña ni tus datos bancarios.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        val provider = selected
        PrimaryButton(
            text = if (provider != null) "Continuar con ${provider.providerLabel()}" else "Continuar",
            onClick = {
                if (provider == null) {
                    error = "Elige un proveedor."
                } else {
                    scope.launch {
                        payoutProviderRegistry.providerFor(provider).linkAccount(trainerId)
                            .onSuccess { url -> onContinue(url, provider) }
                            .onFailure { error = "No se pudo continuar. Inténtalo de nuevo." }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun PayoutStatus.title() = when (this) {
    PayoutStatus.VERIFYING -> "Verificando tu cuenta"
    PayoutStatus.ACTIVE -> "Cuenta vinculada"
    PayoutStatus.ACTION_REQUIRED -> "Falta información"
    PayoutStatus.DISCONNECTED -> "Cuenta desvinculada"
}

/** Estado al volver del proveedor (deep link gestortrainer://payout/return). Se actualiza con su webhook. */
@Composable
fun PayoutLinkStatusScreen(
    trainerId: TrainerId,
    provider: GatewayId,
    payoutProviderRegistry: PayoutProviderRegistry,
    onBackToPayments: () -> Unit,
    onChooseAnother: () -> Unit,
    onReopenProviderPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val payoutProvider = payoutProviderRegistry.providerFor(provider)
    val account by payoutProvider.observeAccount(trainerId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val status = account?.status ?: PayoutStatus.VERIFYING
        Text(status.title(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        when (status) {
            PayoutStatus.VERIFYING -> {
                Text("Proveedor elegido ✓")
                Text("Inicio de sesión ✓")
                Text("Verificación en curso…")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Volver a Habilitación de pagos", onClick = onBackToPayments, modifier = Modifier.weight(1f))
                    SecondaryButton(text = "Elegir otro método", onClick = onChooseAnother, modifier = Modifier.weight(1f))
                }
            }
            PayoutStatus.ACTIVE -> {
                Text("Activa", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(account?.maskedEmail ?: "")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(text = "Ir a Habilitación de pagos", onClick = onBackToPayments, modifier = Modifier.weight(1f))
                    SecondaryButton(text = "Cambiar de cuenta", onClick = onChooseAnother, modifier = Modifier.weight(1f))
                }
            }
            PayoutStatus.ACTION_REQUIRED -> {
                Text("El proveedor necesita más información para verificar tu cuenta.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = "Completar en ${provider.providerLabel()}",
                        onClick = {
                            scope.launch {
                                payoutProvider.linkAccount(trainerId).onSuccess { onReopenProviderPage(it) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(text = "Elegir otro método", onClick = onChooseAnother, modifier = Modifier.weight(1f))
                }
            }
            PayoutStatus.DISCONNECTED -> {
                Text("No tienes una cuenta vinculada.")
                PrimaryButton(text = "Elegir método", onClick = onChooseAnother, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
