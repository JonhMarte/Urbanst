package com.caracolas.gestortrainer.feature.matching.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.TrainerRequestId
import com.caracolas.gestortrainer.feature.payments.api.BillingPeriod
import com.caracolas.gestortrainer.feature.payments.api.PaymentResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Objetivos de la solicitud de contacto desde el descubrimiento. Distinto de FitnessGoal (cuestionario). */
enum class RequestGoal { FUERZA, HIPERTROFIA, PERDER_GRASA, MOVILIDAD }

/**
 * Qué comparte el alumno con el trainer para esta solicitud en concreto.
 * Todo apagado por defecto; revocable luego desde el perfil del alumno.
 */
data class ShareFlags(
    val questionnaireShared: Boolean = false,
    val photoShared: Boolean = false,
    val bodyDataShared: Boolean = false,
)

enum class TrainerRequestStatus { NEW, PROPOSAL_SENT, REJECTED, ACCEPTED }

sealed interface ProposalPayment {
    data class OneOff(val amount: Money) : ProposalPayment
    data class Recurring(val period: BillingPeriod, val amount: Money) : ProposalPayment
}

data class Proposal(
    val title: String,
    val description: String,
    val durationLabel: String,
    val payment: ProposalPayment,
    val expiresAt: Instant,
)

data class TrainerRequest(
    val id: TrainerRequestId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val message: String,
    val goals: List<RequestGoal>,
    val shareFlags: ShareFlags,
    val status: TrainerRequestStatus,
    val proposal: Proposal?,
    val createdAt: Instant,
)

enum class MatchingError { TRAINER_NOT_FOUND, REQUEST_NOT_FOUND, ALREADY_LINKED, PROPOSAL_EXPIRED, NOT_IN_NEW_STATUS, NO_PROPOSAL }

class MatchingException(val error: MatchingError) : Exception()

/**
 * Pipeline "solicitud -> propuesta -> pago" del descubrimiento. Va EN PARALELO al vínculo por ID
 * (feature:clients / LinkService), que no se toca. Al pagar la propuesta se publica ClientLinked.
 */
interface MatchingService {
    suspend fun sendRequest(
        clientId: ClientId,
        trainerPublicId: PublicId,
        message: String,
        goals: List<RequestGoal>,
        shareFlags: ShareFlags,
    ): Result<TrainerRequestId>

    fun observeForTrainer(trainerId: TrainerId): Flow<List<TrainerRequest>>
    fun observeForClient(clientId: ClientId): Flow<List<TrainerRequest>>

    suspend fun reject(requestId: TrainerRequestId): Result<Unit>
    suspend fun sendProposal(requestId: TrainerRequestId, proposal: Proposal): Result<Unit>

    /** Acepta la propuesta y cobra con el proveedor de pago (test). Si tiene éxito, vincula al alumno. */
    suspend fun acceptAndPay(requestId: TrainerRequestId): PaymentResult

    /** El alumno cambia qué comparte para una solicitud ya enviada (p.ej. revocar el cuestionario). */
    suspend fun updateShareFlags(requestId: TrainerRequestId, flags: ShareFlags): Result<Unit>
}
