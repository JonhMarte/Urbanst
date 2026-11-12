package com.caracolas.gestortrainer.feature.payments.impl

import com.caracolas.gestortrainer.feature.payments.api.EntitlementService
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PaymentGateway
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestService
import com.caracolas.gestortrainer.feature.payments.api.PaymentRouter
import com.caracolas.gestortrainer.feature.payments.api.PayoutProvider
import com.caracolas.gestortrainer.feature.payments.api.PayoutProviderRegistry
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PaymentsModule {
    @Binds abstract fun paymentRequestService(impl: InMemoryPaymentRequestService): PaymentRequestService
    @Binds abstract fun entitlementService(impl: InMemoryPaymentRequestService): EntitlementService
    @Binds abstract fun paymentRouter(impl: DefaultPaymentRouter): PaymentRouter
    @Binds abstract fun payoutProviderRegistry(impl: DefaultPayoutProviderRegistry): PayoutProviderRegistry
    @Binds abstract fun paymentGateway(impl: FakePaymentGateway): PaymentGateway

    companion object {
        @Provides @IntoSet @Singleton
        fun payPalPayoutProvider(): PayoutProvider = FakePayoutProvider(GatewayId.PAYPAL)

        @Provides @IntoSet @Singleton
        fun stripePayoutProvider(): PayoutProvider = FakePayoutProvider(GatewayId.STRIPE)

        @Provides @IntoSet @Singleton
        fun mercadoPagoPayoutProvider(): PayoutProvider = FakePayoutProvider(GatewayId.MERCADO_PAGO)
    }
}
