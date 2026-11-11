package com.caracolas.gestortrainer.core.network

/** Errores de red, independientes de si vienen de IO, HTTP o de (de)serialización. */
sealed class NetworkError {
    data object NoConnection : NetworkError()
    data object Timeout : NetworkError()
    data class Http(val code: Int, val errorBody: ApiErrorBody?) : NetworkError()
    data object Serialization : NetworkError()
    data class Unknown(val cause: Throwable) : NetworkError()
}

class NetworkException(val error: NetworkError) : Exception()
