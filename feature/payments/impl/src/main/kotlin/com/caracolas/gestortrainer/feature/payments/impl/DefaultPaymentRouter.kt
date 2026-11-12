package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.feature.payments.api.Delivery
import com.caracolas.gestortrainer.feature.payments.api.PaymentGateway
import com.caracolas.gestortrainer.feature.payments.api.PaymentRouter
import com.caracolas.gestortrainer.feature.payments.api.Product
import javax.inject.Inject

/**
 * Google Play Billing no admite montos libres ni periodo diario, y para coaching online (servicio
 * digital) la política de Play exige su sistema de pago salvo excepciones regionales vigentes.
 * Hasta que Jonathan decida cómo encajar esto (niveles fijos en Play, monto libre solo presencial,
 * o pagos externos donde Google lo permita), este router no enruta nada a PLAY_BILLING: todo pasa
 * por el gateway de prueba. No publicar en Play con cobro online hasta esa decisión.
 */
class DefaultPaymentRouter @Inject constructor(
    private val fakeGateway: FakePaymentGateway,
) : PaymentRouter {
    override fun gatewayFor(product: Product, region: Region, delivery: Delivery): PaymentGateway = fakeGateway
}
