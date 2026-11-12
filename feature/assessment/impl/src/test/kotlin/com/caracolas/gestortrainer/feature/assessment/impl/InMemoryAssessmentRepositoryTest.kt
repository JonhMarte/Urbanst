package com.caracolas.gestortrainer.feature.assessment.impl

import com.caracolas.gestortrainer.core.events.AssessmentUpdated
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.events.on
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.feature.assessment.api.ClientAssessment
import com.caracolas.gestortrainer.feature.assessment.api.Obstacle
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryAssessmentRepositoryTest {
    private val eventBus = InMemoryEventBus()
    private val repository = InMemoryAssessmentRepository(eventBus)

    @Test
    fun alPrincipioNoHayCuestionario() = runTest {
        assertNull(repository.observe(ClientId("c1")).first())
    }

    @Test
    fun guardarLoDejaDisponibleYPublicaElEvento() = runTest(UnconfinedTestDispatcher()) {
        val received = async { eventBus.on<AssessmentUpdated>().first() }

        repository.save(ClientAssessment(clientId = ClientId("c1"), email = "a@b.com"))

        assertEquals(ClientId("c1"), repository.observe(ClientId("c1")).first()?.clientId)
        assertEquals(ClientId("c1"), received.await().clientId)
    }

    @Test
    fun guardarConFrecuenciaAltaVaciaLosObstaculos() = runTest {
        repository.save(
            ClientAssessment(clientId = ClientId("c1"), activityFrequency = 8, obstacles = setOf(Obstacle.TIREDNESS), email = "a@b.com"),
        )

        assertEquals(emptySet<Obstacle>(), repository.observe(ClientId("c1")).first()?.obstacles)
    }
}
