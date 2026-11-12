package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.feature.payments.api.CheckoutRequest
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PaymentGateway
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
import java.util.UUID
import javax.inject.Inject

/**
 * Simula el checkout en la pantalla del proveedor (Stripe en modo test): la app nunca ve
 * los datos de la tarjeta. Siempre tiene éxito, mientras no haya un proveedor real conectado.
 */
class FakePaymentGateway @Inject constructor() : PaymentGateway {
    override val id: GatewayId = GatewayId.STRIPE

    override suspend fun checkout(request: CheckoutRequest): PaymentResult =
        PaymentResult.Success(paymentId = UUID.randomUUID().toString())
}
