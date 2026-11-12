package com.caracolas.gestortrainer.feature.messaging.impl

import com.caracolas.gestortrainer.core.events.ClientLinked
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.feature.messaging.api.MessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Crea la conversación trainer-alumno en cuanto se acepta el vínculo. */
@Singleton
class MessagingLinkCoordinator @Inject constructor(
    private val eventBus: EventBus,
    private val messagingService: MessagingService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            eventBus.on<ClientLinked>().collect { handle(it) }
        }
    }

    internal suspend fun handle(event: ClientLinked) {
        messagingService.conversationWith(event.trainerId, event.clientId)
    }
}
