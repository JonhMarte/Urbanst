package com.caracolas.gestortrainer.core.events

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.TrainerId
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryEventBusTest {
    @Test
    fun `un suscriptor recibe solo el tipo de evento que escucha`() = runTest(UnconfinedTestDispatcher()) {
        val bus = InMemoryEventBus()
        val received = async { bus.on<ClientLinked>().first() }
        val event = ClientLinked(TrainerId("t1"), ClientId("c1"), Instant.EPOCH)
        bus.publish(event)
        assertEquals(event, received.await())
    }
}
