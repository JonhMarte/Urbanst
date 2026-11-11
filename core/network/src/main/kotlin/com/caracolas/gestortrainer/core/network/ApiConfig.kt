package com.caracolas.gestortrainer.core.network

/** Configuración del cliente HTTP que consume [RetrofitFactory]. */
data class ApiConfig(val baseUrl: String, val timeoutSeconds: Long = 30)
