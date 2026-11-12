package com.caracolas.gestortrainer.feature.messaging.api

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.ConversationId
import com.caracolas.gestortrainer.core.model.MessageId
import com.caracolas.gestortrainer.core.model.PageRequest
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class Message(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderId: UserId,
    val text: String,
    val sentAt: Instant,
    val readAt: Instant?,
)

data class Conversation(
    val id: ConversationId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val lastMessage: Message?,
    val unread: Int,
    /** Si el vínculo se rompió, la conversación queda en solo lectura. */
    val readOnly: Boolean = false,
)

enum class MessagingError { NOT_LINKED, TEXT_TOO_LONG, BLANK_TEXT, CONVERSATION_NOT_FOUND, READ_ONLY }

class MessagingException(val error: MessagingError) : Exception()

/** Solo entre un trainer y un alumno vinculados. MVP: solo texto, sin adjuntos. */
interface MessagingService {
    fun observeConversations(userId: UserId): Flow<List<Conversation>>
    fun observeMessages(conversationId: ConversationId, page: PageRequest = PageRequest()): Flow<List<Message>>
    suspend fun send(conversationId: ConversationId, senderId: UserId, text: String): Result<MessageId>
    suspend fun markRead(conversationId: ConversationId, readerId: UserId): Result<Unit>
    suspend fun conversationWith(trainerId: TrainerId, clientId: ClientId): Result<ConversationId>

    companion object {
        const val MAX_TEXT_LENGTH = 2000
    }
}
