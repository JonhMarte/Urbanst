package com.caracolas.gestortrainer.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Construye el cliente Retrofit único de la app a partir de [ApiConfig]. */
class RetrofitFactory(
    private val config: ApiConfig,
    private val authTokenProvider: AuthTokenProvider,
    private val debug: Boolean,
) {
    fun create(): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor())
            .apply {
                if (debug) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(NetworkJson.instance.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun authInterceptor() = Interceptor { chain ->
        val token = runBlocking { authTokenProvider.currentToken() }
        val request = chain.request().newBuilder()
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()
        chain.proceed(request)
    }
}
