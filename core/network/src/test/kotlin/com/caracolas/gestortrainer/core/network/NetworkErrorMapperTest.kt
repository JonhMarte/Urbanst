package com.caracolas.gestortrainer.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class NetworkErrorMapperTest {

    private val mapper = NetworkErrorMapper(Json)

    @Test
    fun `sin conexion se traduce a NoConnection`() {
        assertEquals(NetworkError.NoConnection, mapper.map(IOException()))
    }

    @Test
    fun `timeout se traduce a Timeout`() {
        assertEquals(NetworkError.Timeout, mapper.map(SocketTimeoutException()))
    }

    @Test
    fun `error de serializacion se traduce a Serialization`() {
        assertEquals(NetworkError.Serialization, mapper.map(SerializationException("boom")))
    }

    @Test
    fun `http 404 con cuerpo de error expone el codigo y el cuerpo parseado`() {
        val body = """{"code":"NOT_FOUND","message":"No existe"}"""
            .toResponseBody("application/json".toMediaType())
        val httpException = HttpException(Response.error<Any>(404, body))

        val result = mapper.map(httpException) as NetworkError.Http

        assertEquals(404, result.code)
        assertEquals("NOT_FOUND", result.errorBody?.code)
    }

    @Test
    fun `cualquier otra excepcion se traduce a Unknown conservando la causa`() {
        val cause = IllegalStateException("boom")

        val result = mapper.map(cause) as NetworkError.Unknown

        assertTrue(result.cause === cause)
    }
}
