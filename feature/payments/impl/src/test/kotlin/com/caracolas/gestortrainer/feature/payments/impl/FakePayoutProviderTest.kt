package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PayoutStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakePayoutProviderTest {
    private val trainerId = TrainerId("t1")

    @Test
    fun sinVincularNoHayCuenta() = runTest {
        val provider = FakePayoutProvider(GatewayId.PAYPAL)
        assertNull(provider.observeAccount(trainerId).first())
    }

    @Test
    fun vincularDejaLaCuentaVerificando() = runTest {
        val provider = FakePayoutProvider(GatewayId.STRIPE)

        provider.linkAccount(trainerId)

        val account = provider.observeAccount(trainerId).first()
        assertEquals(PayoutStatus.VERIFYING, account?.status)
        assertEquals(GatewayId.STRIPE, account?.provider)
    }
}

class DefaultPayoutProviderRegistryTest {
    @Test
    fun devuelveElProveedorQueCorrespondeAlId() {
        val paypal = FakePayoutProvider(GatewayId.PAYPAL)
        val stripe = FakePayoutProvider(GatewayId.STRIPE)
        val registry = DefaultPayoutProviderRegistry(setOf(paypal, stripe))

        assertEquals(paypal, registry.providerFor(GatewayId.PAYPAL))
        assertEquals(stripe, registry.providerFor(GatewayId.STRIPE))
    }
}
