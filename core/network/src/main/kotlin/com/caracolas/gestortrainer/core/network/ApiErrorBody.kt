package com.caracolas.gestortrainer.core.network

import kotlinx.serialization.Serializable

/** Cuerpo de error estándar que devuelve el backend (ver ARQUITECTURA.md §7). */
@Serializable
data class ApiErrorBody(val code: String? = null, val message: String? = null)
