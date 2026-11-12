package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.core.model.PaymentRequestId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.payments.api.Delivery
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultPaymentRouterTest {
    @Test
    fun nuncaEnrutaACobroOnlineDeGooglePlayMientrasNoHayaDecision() {
        val router = DefaultPaymentRouter(FakePaymentGateway())
        val product = Product.OneOffRequest(PaymentRequestId("r1"), TrainerId("t1"))

        val onlineGateway = router.gatewayFor(product, Region("ES"), Delivery.ONLINE)
        val inPersonGateway = router.gatewayFor(product, Region("ES"), Delivery.IN_PERSON)

        assertEquals(GatewayId.STRIPE, onlineGateway.id)
        assertEquals(GatewayId.STRIPE, inPersonGateway.id)
    }
}
