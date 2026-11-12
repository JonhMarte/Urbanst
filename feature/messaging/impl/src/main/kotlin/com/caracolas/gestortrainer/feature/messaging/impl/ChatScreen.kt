package com.caracolas.gestortrainer.feature.messaging.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.ConversationId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.messaging.api.Message
import com.caracolas.gestortrainer.feature.messaging.api.MessagingService
import kotlinx.coroutines.launch

/** Mensajería trainer ↔ alumno. MVP: solo texto, máx. 2000 caracteres, sin adjuntos. */
@Composable
fun ChatScreen(
    conversationId: ConversationId,
    currentUserId: UserId,
    otherDisplayName: String,
    messagingService: MessagingService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by messagingService.observeMessages(conversationId).collectAsState(initial = emptyList())
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationId) { messagingService.markRead(conversationId, currentUserId) }

    Column(modifier = modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text(otherDisplayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            messages.forEach { message -> MessageBubble(message, isMine = message.senderId == currentUserId) }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= MessagingService.MAX_TEXT_LENGTH) draft = it },
                placeholder = { Text("Escribe un mensaje…") },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = if (sending) "Enviando…" else "Enviar",
                onClick = {
                    val text = draft
                    sending = true
                    scope.launch {
                        val result = messagingService.send(conversationId, currentUserId, text)
                        sending = false
                        result.onSuccess { draft = ""; error = null }
                        result.onFailure { error = "No se pudo enviar. Inténtalo de nuevo." }
                    }
                },
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier.widthIn(max = 260.dp)
                .background(
                    if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(14.dp),
                )
                .padding(10.dp),
        ) {
            Text(message.text)
            Text(
                if (message.readAt != null) "Leído" else "Enviado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
