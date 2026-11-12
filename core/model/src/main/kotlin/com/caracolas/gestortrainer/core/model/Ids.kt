package com.caracolas.gestortrainer.core.model

@JvmInline value class UserId(val value: String)
@JvmInline value class TrainerId(val value: String)
@JvmInline value class ClientId(val value: String)

/** ID público que se comparte para vincularse (ej. TR-48213, AL-30917). */
@JvmInline value class PublicId(val value: String)

@JvmInline value class ActivityId(val value: String)
@JvmInline value class MuscleId(val value: String)
@JvmInline value class AssignmentId(val value: String)
@JvmInline value class SessionId(val value: String)
@JvmInline value class LinkRequestId(val value: String)
@JvmInline value class NoteId(val value: String)
@JvmInline value class PlanId(val value: String)
@JvmInline value class GroupId(val value: String)
@JvmInline value class ConversationId(val value: String)
@JvmInline value class MessageId(val value: String)
@JvmInline value class PaymentRequestId(val value: String)
@JvmInline value class TrainerRequestId(val value: String)
