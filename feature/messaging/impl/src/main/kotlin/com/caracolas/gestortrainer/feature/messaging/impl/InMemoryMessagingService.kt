package com.caracolas.gestortrainer.feature.messaging.impl

import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.events.MessageSent
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.ConversationId
import com.caracolas.gestortrainer.core.model.MessageId
import com.caracolas.gestortrainer.core.model.PageRequest
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.messaging.api.Conversation
import com.caracolas.gestortrainer.feature.messaging.api.Message
import com.caracolas.gestortrainer.feature.messaging.api.MessagingError
import com.caracolas.gestortrainer.feature.messaging.api.MessagingException
import com.caracolas.gestortrainer.feature.messaging.api.MessagingService
import com.caracolas.gestortrainer.feature.messaging.api.MessagingService.Companion.MAX_TEXT_LENGTH
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private data class ConversationRecord(
    val id: ConversationId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val messages: List<Message> = emptyList(),
    val readOnly: Boolean = false,
    val trainerLastReadAt: Instant? = null,
    val clientLastReadAt: Instant? = null,
)

/** Mensajería trainer-alumno en memoria mientras no exista el backend (WebSocket/AppSync + DynamoDB). */
@Singleton
class InMemoryMessagingService @Inject constructor(
    private val eventBus: EventBus,
) : MessagingService {
    private val conversations = MutableStateFlow<Map<ConversationId, ConversationRecord>>(emptyMap())

    override fun observeConversations(userId: UserId): Flow<List<Conversation>> = conversations.map { all ->
        all.values
            .filter { it.trainerId.value == userId.value || it.clientId.value == userId.value }
            .map { it.toConversation(userId) }
    }

    override fun observeMessages(conversationId: ConversationId, page: PageRequest): Flow<List<Message>> =
        conversations.map { all ->
            all[conversationId]?.messages
                ?.drop(page.page * page.size)
                ?.take(page.size)
                .orEmpty()
        }

    override suspend fun send(conversationId: ConversationId, senderId: UserId, text: String): Result<MessageId> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(MessagingException(MessagingError.BLANK_TEXT))
        if (trimmed.length > MAX_TEXT_LENGTH) return Result.failure(MessagingException(MessagingError.TEXT_TOO_LONG))
        val record = conversations.value[conversationId]
            ?: return Result.failure(MessagingException(MessagingError.CONVERSATION_NOT_FOUND))
        if (record.readOnly) return Result.failure(MessagingException(MessagingError.READ_ONLY))

        val message = Message(MessageId(UUID.randomUUID().toString()), conversationId, senderId, trimmed, Instant.now(), null)
        conversations.value = conversations.value + (conversationId to record.copy(messages = record.messages + message))
        eventBus.publish(MessageSent(conversationId, message.id, senderId, message.sentAt))
        return Result.success(message.id)
    }

    override suspend fun markRead(conversationId: ConversationId, readerId: UserId): Result<Unit> {
        val record = conversations.value[conversationId]
            ?: return Result.failure(MessagingException(MessagingError.CONVERSATION_NOT_FOUND))
        val now = Instant.now()
        val updated = when (readerId.value) {
            record.trainerId.value -> record.copy(trainerLastReadAt = now)
            record.clientId.value -> record.copy(clientLastReadAt = now)
            else -> record
        }
        conversations.value = conversations.value + (conversationId to updated)
        return Result.success(Unit)
    }

    override suspend fun conversationWith(trainerId: TrainerId, clientId: ClientId): Result<ConversationId> {
        val existing = conversations.value.values.find { it.trainerId == trainerId && it.clientId == clientId }
        if (existing != null) return Result.success(existing.id)

        val id = ConversationId(UUID.randomUUID().toString())
        conversations.value = conversations.value + (id to ConversationRecord(id, trainerId, clientId))
        return Result.success(id)
    }

    private fun ConversationRecord.toConversation(viewerId: UserId): Conversation {
        val lastReadAt = if (viewerId.value == trainerId.value) trainerLastReadAt else clientLastReadAt
        val unread = messages.count { it.senderId.value != viewerId.value && (lastReadAt == null || it.sentAt.isAfter(lastReadAt)) }
        return Conversation(id, trainerId, clientId, messages.lastOrNull(), unread, readOnly)
    }
}
