package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.payments.api.Balance
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PayoutAccount
import com.caracolas.gestortrainer.feature.payments.api.PayoutProvider
import com.caracolas.gestortrainer.feature.payments.api.PayoutStatus
import com.caracolas.gestortrainer.core.model.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Simula el flujo de vincular cuenta de cobro mientras no exista un proveedor real.
 * Guarda solo el ID de la cuenta del proveedor, nunca datos bancarios.
 * No hay webhook real: tras "verificar" (VERIFYING) pasa a ACTIVE sola, a los pocos segundos.
 */
class FakePayoutProvider(override val id: GatewayId) : PayoutProvider {
    private val accounts = MutableStateFlow<Map<TrainerId, PayoutAccount>>(emptyMap())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun linkAccount(trainerId: TrainerId): Result<String> {
        accounts.value = accounts.value + (trainerId to PayoutAccount(id, maskedEmail = null, status = PayoutStatus.VERIFYING))
        scope.launch {
            delay(VERIFICATION_DELAY_MS)
            val current = accounts.value[trainerId] ?: return@launch
            if (current.status == PayoutStatus.VERIFYING) {
                accounts.value = accounts.value + (trainerId to current.copy(maskedEmail = "tr****@${id.name.lowercase()}.com", status = PayoutStatus.ACTIVE))
            }
        }
        return Result.success("https://sandbox.${id.name.lowercase()}.example.com/connect?trainer=${trainerId.value}")
    }

    override fun observeAccount(trainerId: TrainerId): Flow<PayoutAccount?> = accounts.map { it[trainerId] }

    override fun observeBalance(trainerId: TrainerId): Flow<Balance> =
        accounts.map { Balance(Money(0, "EUR"), Money(0, "EUR")) }

    companion object {
        private const val VERIFICATION_DELAY_MS = 2500L
    }
}
