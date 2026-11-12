package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PayoutProvider
import com.caracolas.gestortrainer.feature.payments.api.PayoutProviderRegistry
import javax.inject.Inject

class DefaultPayoutProviderRegistry @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards PayoutProvider>,
) : PayoutProviderRegistry {
    override fun providerFor(id: GatewayId): PayoutProvider =
        providers.find { it.id == id } ?: error("No hay PayoutProvider registrado para $id")
}
