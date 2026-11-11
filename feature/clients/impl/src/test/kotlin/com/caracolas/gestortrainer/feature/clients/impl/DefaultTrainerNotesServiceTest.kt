package com.caracolas.gestortrainer.feature.clients.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.TrainerId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTrainerNotesServiceTest {
    private val service = DefaultTrainerNotesService()

    @Test
    fun unaNotaValidaQuedaDisponibleParaElAlumno() = runBlocking {
        val result = service.addNote(TrainerId("t1"), ClientId("c1"), "Baja un poco el peso y cuida la espalda.")

        assertTrue(result.isSuccess)
        val notes = service.observeNotes(ClientId("c1")).first()
        assertEquals(1, notes.size)
        assertEquals("Baja un poco el peso y cuida la espalda.", notes.first().text)
    }

    @Test
    fun unaNotaVaciaFalla() = runBlocking {
        val result = service.addNote(TrainerId("t1"), ClientId("c1"), "   ")

        assertTrue(result.isFailure)
        assertTrue(service.observeNotes(ClientId("c1")).first().isEmpty())
    }

    @Test
    fun lasNotasSeFiltranPorAlumnoYLaMasRecienteVaPrimero() = runBlocking {
        service.addNote(TrainerId("t1"), ClientId("c1"), "Primera nota")
        service.addNote(TrainerId("t1"), ClientId("otro"), "Nota de otro alumno")
        service.addNote(TrainerId("t1"), ClientId("c1"), "Segunda nota")

        val notes = service.observeNotes(ClientId("c1")).first()

        assertEquals(2, notes.size)
        assertEquals("Segunda nota", notes.first().text)
        assertTrue(notes.all { it.clientId == ClientId("c1") })
    }
}
