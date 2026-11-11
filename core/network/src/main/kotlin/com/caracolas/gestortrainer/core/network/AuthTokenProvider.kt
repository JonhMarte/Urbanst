package com.caracolas.gestortrainer.core.network

/** Lo implementa core/auth; se mantiene aquí para que network no dependa de auth. */
fun interface AuthTokenProvider {
    suspend fun currentToken(): String?
}
