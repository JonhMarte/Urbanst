package com.caracolas.gestortrainer.core.model

import java.time.LocalDate

enum class Role { TRAINER, CLIENT }

data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) { "El rango de fechas está invertido" } }
}

data class PageRequest(val page: Int = 0, val size: Int = 20)

/** Importe en la unidad mínima de la moneda (céntimos) para evitar errores de redondeo. */
data class Money(val amountMinor: Long, val currency: String)

/** País (ISO 3166-1 alfa-2) o trabajo remoto. */
data class Region(val countryCode: String) {
    val isRemote: Boolean get() = countryCode == REMOTE
    companion object { const val REMOTE = "REMOTE" }
}
