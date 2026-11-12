package com.caracolas.gestortrainer.feature.payments.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PaymentRequestId
import com.caracolas.gestortrainer.core.model.PlanId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.TrainerRequestId
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
    /** Solicitud de pago puntual que crea el trainer (diario/mensual/semestral/anual). */
    data class OneOffRequest(val requestId: PaymentRequestId, val trainerId: TrainerId) : Product
    /** Propuesta aceptada en el pipeline de descubrimiento (feature:matching). Al cobrar, vincula al alumno. */
    data class MatchingProposal(val requestId: TrainerRequestId, val trainerId: TrainerId) : Product
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

enum class PayoutStatus { VERIFYING, ACTIVE, ACTION_REQUIRED, DISCONNECTED }

data class PayoutAccount(val provider: GatewayId, val maskedEmail: String?, val status: PayoutStatus)

/** Cómo cobra el trainer. Un proveedor (PayPal, Stripe Connect, Mercado Pago), elegido con [PayoutProviderRegistry]. */
interface PayoutProvider {
    val id: GatewayId

    /** Devuelve la URL del flujo del proveedor para vincular la cuenta (se abre en una Custom Tab). */
    suspend fun linkAccount(trainerId: TrainerId): Result<String>
    fun observeAccount(trainerId: TrainerId): Flow<PayoutAccount?>
    fun observeBalance(trainerId: TrainerId): Flow<Balance>
}

/** Elige la implementación de [PayoutProvider] según el proveedor que escoja el trainer. */
interface PayoutProviderRegistry {
    fun providerFor(id: GatewayId): PayoutProvider
}

data class Entitlement(val productKey: String, val expiresAt: Instant?)

/** Qué tiene pagado cada usuario. La fuente de verdad es el backend. */
interface EntitlementService {
    fun observe(userId: UserId): Flow<List<Entitlement>>
}

enum class BillingPeriod(val days: Int) { DAILY(1), MONTHLY(30), SEMIANNUAL(182), ANNUAL(365) }

enum class PaymentRequestStatus { DRAFT, SENT, PAID, CANCELLED, EXPIRED }

data class PaymentRequest(
    val id: PaymentRequestId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val period: BillingPeriod,
    val amount: Money,
    val status: PaymentRequestStatus,
    val createdAt: Instant,
    val expiresAt: Instant?,
)

enum class PaymentRequestError { CLIENT_NOT_LINKED, INVALID_AMOUNT, NOT_FOUND, NO_PAYOUT_METHOD }

class PaymentRequestException(val error: PaymentRequestError) : Exception()

/**
 * El trainer crea una solicitud de pago puntual para un alumno vinculado (diario/mensual/semestral/anual).
 * Al enviar una nueva, la anterior SENT para ese alumno pasa a CANCELLED. Expira a los 7 días sin pagar.
 */
interface PaymentRequestService {
    suspend fun saveDraft(trainerId: TrainerId, clientPublicId: PublicId, period: BillingPeriod, amount: Money): Result<PaymentRequestId>
    suspend fun send(id: PaymentRequestId): Result<Unit>
    suspend fun cancel(id: PaymentRequestId): Result<Unit>
    fun observeForTrainer(trainerId: TrainerId): Flow<List<PaymentRequest>>
    fun observePendingForClient(clientId: ClientId): Flow<List<PaymentRequest>>
    /** Inicia el pago con el proveedor que decida PaymentRouter. */
    suspend fun pay(id: PaymentRequestId): PaymentResult

    companion object {
        const val MAX_AMOUNT_MINOR = 100_000_00L // 100 000,00 en la unidad mínima de la moneda
        const val EXPIRES_AFTER_DAYS = 7L
    }
}
