package com.caracolas.gestortrainer.core.auth

import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.flow.Flow

data class Session(val userId: UserId, val publicId: PublicId, val role: Role)

/** Sesión del usuario (Cognito en la implementación real). */
interface SessionRepository {
    val session: Flow<Session?>
    suspend fun signIn(email: String, password: String): Result<Session>
    suspend fun signOut()

    /** La abre el registro (sin backend real todavía); Cognito traerá el inicio de sesión con credenciales. */
    suspend fun startSession(session: Session): Result<Unit>
}
