package com.caracolas.gestortrainer.feature.clients.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.NoteId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.clients.api.Note
import com.caracolas.gestortrainer.feature.clients.api.TrainerNotesService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Notas del trainer para un alumno, en memoria mientras no exista el backend AWS (Fase 1). */
@Singleton
class DefaultTrainerNotesService @Inject constructor() : TrainerNotesService {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    override suspend fun addNote(trainerId: TrainerId, clientId: ClientId, text: String): Result<NoteId> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("La nota no puede estar vacía"))
        val note = Note(NoteId(UUID.randomUUID().toString()), trainerId, clientId, text.trim(), Instant.now())
        notes.value = notes.value + note
        return Result.success(note.id)
    }

    /** Más reciente primero; como solo se añade al final, basta con invertir el orden de inserción. */
    override fun observeNotes(clientId: ClientId): Flow<List<Note>> =
        notes.map { list -> list.filter { it.clientId == clientId }.asReversed() }
}
