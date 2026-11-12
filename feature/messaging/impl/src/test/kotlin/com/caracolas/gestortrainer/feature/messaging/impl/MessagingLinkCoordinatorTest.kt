package com.caracolas.gestortrainer.feature.messaging.impl

import com.caracolas.gestortrainer.core.events.ClientLinked
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

class MessagingLinkCoordinatorTest {
    @Test
    fun aceptarUnVinculoCreaLaConversacion() = runTest {
        val eventBus = InMemoryEventBus()
        val service = InMemoryMessagingService(eventBus)
        val coordinator = MessagingLinkCoordinator(eventBus, service)

        coordinator.handle(ClientLinked(TrainerId("t1"), ClientId("c1"), Instant.now()))

        val conversations = service.observeConversations(UserId("t1")).first()
        assertNotNull(conversations.firstOrNull())
    }
}
