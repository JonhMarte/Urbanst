package com.caracolas.gestortrainer.feature.payments.api

import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PlanId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

enum class Tier { PRO }

sealed interface Product {
    /** Membresía del alumno con un trainer. */
    data class ClientMembership(val trainerId: TrainerId, val plan: PlanId) : Product
    /** Mes pagado al contado. */
    data class ClientMonthly(val trainerId: TrainerId) : Product
    /** Suscripción del trainer a la app. */
    data class TrainerPro(val tier: Tier) : Product
}

/** Cómo se presta el servicio: decide si Google Play exige su sistema de cobro. */
enum class Delivery { ONLINE, IN_PERSON }

enum class GatewayId { PLAY_BILLING, STRIPE, MERCADO_PAGO, PAYPAL }

data class CheckoutRequest(val userId: UserId, val product: Product, val region: Region, val delivery: Delivery)

sealed interface PaymentResult {
    data class Success(val paymentId: String) : PaymentResult
    data object Cancelled : PaymentResult
    data class Failed(val reason: String) : PaymentResult
}

/** Una implementación por proveedor. La app nunca guarda datos de tarjeta: solo tokens del proveedor. */
interface PaymentGateway {
    val id: GatewayId
    suspend fun checkout(request: CheckoutRequest): PaymentResult
}

/** Decide el proveedor según producto, país y reglas de Google Play. */
interface PaymentRouter {
    fun gatewayFor(product: Product, region: Region, delivery: Delivery): PaymentGateway
}

data class Balance(val available: Money, val pending: Money)

/** Cómo cobra el trainer. MVP: PayPal. */
interface PayoutProvider {
    /** Devuelve la URL del flujo del proveedor para vincular la cuenta. */
    suspend fun linkAccount(trainerId: TrainerId): Result<String>
    fun observeBalance(trainerId: TrainerId): Flow<Balance>
}

data class Entitlement(val productKey: String, val expiresAt: Instant?)

/** Qué tiene pagado cada usuario. La fuente de verdad es el backend. */
interface EntitlementService {
    fun observe(userId: UserId): Flow<List<Entitlement>>
}
