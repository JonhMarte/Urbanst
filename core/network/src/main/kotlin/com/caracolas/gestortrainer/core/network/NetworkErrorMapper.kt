package com.caracolas.gestortrainer.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** Traduce cualquier excepción de red/HTTP/serialización al [NetworkError] de dominio. */
class NetworkErrorMapper(private val json: Json) {

    fun map(throwable: Throwable): NetworkError = when (throwable) {
        is SocketTimeoutException -> NetworkError.Timeout
        is HttpException -> NetworkError.Http(throwable.code(), parseErrorBody(throwable))
        is SerializationException -> NetworkError.Serialization
        is IOException -> NetworkError.NoConnection
        else -> NetworkError.Unknown(throwable)
    }

    private fun parseErrorBody(httpException: HttpException): ApiErrorBody? {
        val raw = httpException.response()?.errorBody()?.string() ?: return null
        return runCatching { json.decodeFromString(ApiErrorBody.serializer(), raw) }.getOrNull()
    }
}
