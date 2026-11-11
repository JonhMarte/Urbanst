package com.caracolas.gestortrainer.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Implementación en memoria mientras no exista el backend de Cognito (Fase 1). */
class InMemorySessionRepository : SessionRepository {
    private val state = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = state

    override suspend fun signIn(email: String, password: String): Result<Session> =
        Result.failure(UnsupportedOperationException("El inicio de sesión con credenciales llega con el backend de Cognito"))

    override suspend fun signOut() {
        state.value = null
    }

    override suspend fun startSession(session: Session): Result<Unit> {
        state.value = session
        return Result.success(Unit)
    }
}
