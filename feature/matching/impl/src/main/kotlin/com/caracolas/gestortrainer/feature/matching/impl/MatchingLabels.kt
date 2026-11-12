package com.caracolas.gestortrainer.feature.matching.impl

import com.caracolas.gestortrainer.feature.matching.api.RequestGoal
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequestStatus
import com.caracolas.gestortrainer.feature.payments.api.BillingPeriod

internal fun RequestGoal.label(): String = when (this) {
    RequestGoal.FUERZA -> "Fuerza"
    RequestGoal.HIPERTROFIA -> "Hipertrofia"
    RequestGoal.PERDER_GRASA -> "Perder grasa"
    RequestGoal.MOVILIDAD -> "Movilidad"
}

internal fun TrainerRequestStatus.label(): String = when (this) {
    TrainerRequestStatus.NEW -> "Nueva"
    TrainerRequestStatus.PROPOSAL_SENT -> "Propuesta enviada"
    TrainerRequestStatus.REJECTED -> "Rechazada"
    TrainerRequestStatus.ACCEPTED -> "Aceptada"
}

internal fun BillingPeriod.matchingLabel(): String = when (this) {
    BillingPeriod.DAILY -> "Diario"
    BillingPeriod.MONTHLY -> "Mensual"
    BillingPeriod.SEMIANNUAL -> "Semestral"
    BillingPeriod.ANNUAL -> "Anual"
}
