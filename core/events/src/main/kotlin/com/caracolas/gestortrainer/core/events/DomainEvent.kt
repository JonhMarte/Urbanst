package com.caracolas.gestortrainer.core.events

import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.AssignmentId
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.ConversationId
import com.caracolas.gestortrainer.core.model.MessageId
import com.caracolas.gestortrainer.core.model.Money
import com.caracolas.gestortrainer.core.model.PaymentRequestId
import com.caracolas.gestortrainer.core.model.SessionId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import java.time.Instant
import java.time.LocalDate

/** Hechos del dominio. Los módulos se comunican publicándolos, no llamándose entre sí. */
sealed interface DomainEvent {
    val occurredAt: Instant
}

data class LinkRequested(
    val trainerId: TrainerId,
    val clientId: ClientId,
    override val occurredAt: Instant,
) : DomainEvent

data class ClientLinked(
    val trainerId: TrainerId,
    val clientId: ClientId,
    override val occurredAt: Instant,
) : DomainEvent

data class ActivityAssigned(
    val assignmentId: AssignmentId,
    val activityId: ActivityId,
    val clientId: ClientId,
    val date: LocalDate,
    override val occurredAt: Instant,
) : DomainEvent

/** Inicio y fin los pone el servidor, no el reloj del teléfono. */
data class ActivitySessionCompleted(
    val sessionId: SessionId,
    val assignmentId: AssignmentId,
    val clientId: ClientId,
    val startedAt: Instant,
    val finishedAt: Instant,
    val hasPhoto: Boolean,
    override val occurredAt: Instant,
) : DomainEvent

data class ScoreAwarded(
    val userId: UserId,
    val points: Int,
    val ruleId: String,
    override val occurredAt: Instant,
) : DomainEvent

data class ReviewSubmitted(
    val trainerId: TrainerId,
    val clientId: ClientId,
    val stars: Int,
    override val occurredAt: Instant,
) : DomainEvent

data class PaymentSucceeded(
    val userId: UserId,
    val paymentId: String,
    val amount: Money,
    override val occurredAt: Instant,
) : DomainEvent

data class RefundIssued(
    val userId: UserId,
    val paymentId: String,
    override val occurredAt: Instant,
) : DomainEvent

data class AssessmentUpdated(
    val clientId: ClientId,
    override val occurredAt: Instant,
) : DomainEvent

data class MessageSent(
    val conversationId: ConversationId,
    val messageId: MessageId,
    val senderId: UserId,
    override val occurredAt: Instant,
) : DomainEvent

data class PaymentRequested(
    val id: PaymentRequestId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val amount: Money,
    override val occurredAt: Instant,
) : DomainEvent
