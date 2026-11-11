package com.caracolas.gestortrainer.feature.activities.impl

import com.caracolas.gestortrainer.core.auth.InMemorySessionRepository
import com.caracolas.gestortrainer.core.auth.Session
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.activities.api.ActivityDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class InMemoryActivityRepositoryTest {

    @Test
    fun guardarSinSesionActivaFalla() = runBlocking {
        val repository = InMemoryActivityRepository(InMemorySessionRepository())

        val result = repository.save(ActivityDraft(title = "X", description = "Y", averageDuration = 10.minutes))

        assertTrue(result.isFailure)
    }

    @Test
    fun guardarAsignaElTrainerDeLaSesionActivaYApareceEnSuBiblioteca() = runBlocking {
        val sessionRepository = InMemorySessionRepository()
        sessionRepository.startSession(Session(UserId("t1"), PublicId("TR-00001"), Role.TRAINER))
        val repository = InMemoryActivityRepository(sessionRepository)

        val id = repository.save(
            ActivityDraft(title = "Sentadilla", description = "Baja hasta paralelo.", averageDuration = 45.minutes),
        ).getOrThrow()

        val activity = repository.get(id).getOrThrow()
        assertEquals(TrainerId("t1"), activity.trainerId)
        assertEquals(listOf(activity), repository.observeLibrary(TrainerId("t1")).first())
        assertTrue(repository.observeLibrary(TrainerId("otro")).first().isEmpty())
    }

    @Test
    fun borrarQuitaLaActividadDeLaBiblioteca() = runBlocking {
        val sessionRepository = InMemorySessionRepository()
        sessionRepository.startSession(Session(UserId("t1"), PublicId("TR-00001"), Role.TRAINER))
        val repository = InMemoryActivityRepository(sessionRepository)
        val id = repository.save(
            ActivityDraft(title = "Sentadilla", description = "Baja hasta paralelo.", averageDuration = 45.minutes),
        ).getOrThrow()

        repository.delete(id)

        assertTrue(repository.observeLibrary(TrainerId("t1")).first().isEmpty())
    }
}
