package com.caracolas.gestortrainer.core.network

import kotlinx.serialization.json.Json

/** Configuración única de kotlinx.serialization para toda la app. */
object NetworkJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
}
