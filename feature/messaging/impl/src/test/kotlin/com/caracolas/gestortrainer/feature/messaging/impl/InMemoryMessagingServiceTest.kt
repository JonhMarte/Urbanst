package com.caracolas.gestortrainer.feature.messaging.impl

import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.MessageSent
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.ConversationId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.messaging.api.MessagingException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryMessagingServiceTest {
    private val eventBus = InMemoryEventBus()
    private val service = InMemoryMessagingService(eventBus)
    private val trainerId = TrainerId("t1")
    private val clientId = ClientId("c1")

    @Test
    fun conversationWithEsIdempotente() = runTest {
        val first = service.conversationWith(trainerId, clientId).getOrThrow()
        val second = service.conversationWith(trainerId, clientId).getOrThrow()
        assertEquals(first, second)
    }

    @Test
    fun enviarUnMensajeLoDejaComoUltimoYPublicaElEvento() = runTest(UnconfinedTestDispatcher()) {
        val conversationId = service.conversationWith(trainerId, clientId).getOrThrow()
        val received = async { eventBus.on<MessageSent>().first() }

        service.send(conversationId, UserId(trainerId.value), "Hola Laura")

        val messages = service.observeMessages(conversationId).first()
        assertEquals("Hola Laura", messages.last().text)
        assertEquals(conversationId, received.await().conversationId)
    }

    @Test
    fun elDestinatarioVeNoLeidosYElRemitenteNo() = runTest {
        val conversationId = service.conversationWith(trainerId, clientId).getOrThrow()
        service.send(conversationId, UserId(trainerId.value), "Hola")

        val trainerView = service.observeConversations(UserId(trainerId.value)).first().first()
        val clientView = service.observeConversations(UserId(clientId.value)).first().first()

        assertEquals(0, trainerView.unread)
        assertEquals(1, clientView.unread)
    }

    @Test
    fun marcarLeidoPoneLosNoLeidosACero() = runTest {
        val conversationId = service.conversationWith(trainerId, clientId).getOrThrow()
        service.send(conversationId, UserId(trainerId.value), "Hola")

        service.markRead(conversationId, UserId(clientId.value))

        val clientView = service.observeConversations(UserId(clientId.value)).first().first()
        assertEquals(0, clientView.unread)
    }

    @Test
    fun unTextoVacioFalla() = runTest {
        val conversationId = service.conversationWith(trainerId, clientId).getOrThrow()
        val result = service.send(conversationId, UserId(trainerId.value), "   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun unTextoDemasiadoLargoFalla() = runTest {
        val conversationId = service.conversationWith(trainerId, clientId).getOrThrow()
        val result = service.send(conversationId, UserId(trainerId.value), "x".repeat(2001))
        assertTrue(result.isFailure)
    }

    @Test
    fun noSePuedeEscribirAUnaConversacionInexistente() = runTest {
        val result = service.send(ConversationId("no-existe"), UserId(trainerId.value), "hola")
        assertTrue(result.isFailure)
        assertTrue((result.exceptionOrNull() as MessagingException).error.name == "CONVERSATION_NOT_FOUND")
    }
}
