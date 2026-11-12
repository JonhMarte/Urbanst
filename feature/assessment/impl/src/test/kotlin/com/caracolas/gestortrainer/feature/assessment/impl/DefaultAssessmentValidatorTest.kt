package com.caracolas.gestortrainer.feature.assessment.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentError
import com.caracolas.gestortrainer.feature.assessment.api.ClientAssessment
import com.caracolas.gestortrainer.feature.assessment.api.DailyTime
import com.caracolas.gestortrainer.feature.assessment.api.Obstacle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAssessmentValidatorTest {
    private val validator = DefaultAssessmentValidator()

    private fun base() = ClientAssessment(
        clientId = ClientId("c1"),
        activityFrequency = 5,
        daysPerWeek = 3,
        dailyTime = DailyTime.MIN_30,
        email = "alumno@example.com",
    )

    @Test
    fun unCuestionarioValidoNoTieneErrores() {
        assertTrue(validator.validate(base()).isEmpty())
    }

    @Test
    fun frecuenciaFueraDeRangoFalla() {
        val errors = validator.validate(base().copy(activityFrequency = 11))
        assertTrue(AssessmentError.ACTIVITY_FREQUENCY_OUT_OF_RANGE in errors)
    }

    @Test
    fun diasPorSemanaFueraDeRangoFalla() {
        val errors = validator.validate(base().copy(daysPerWeek = 7))
        assertTrue(AssessmentError.DAYS_PER_WEEK_OUT_OF_RANGE in errors)
    }

    @Test
    fun problemaPsicomotrizSinDetalleFalla() {
        val errors = validator.validate(base().copy(psychomotorIssue = true, psychomotorDetails = null))
        assertTrue(AssessmentError.PSYCHOMOTOR_DETAILS_REQUIRED in errors)
    }

    @Test
    fun problemaPsicomotrizConDetalleDemasiadoLargoFalla() {
        val errors = validator.validate(base().copy(psychomotorIssue = true, psychomotorDetails = "x".repeat(501)))
        assertTrue(AssessmentError.PSYCHOMOTOR_DETAILS_TOO_LONG in errors)
    }

    @Test
    fun problemaPsicomotrizConDetalleValidoNoFalla() {
        val errors = validator.validate(base().copy(psychomotorIssue = true, psychomotorDetails = "Lesión de rodilla"))
        assertTrue(AssessmentError.PSYCHOMOTOR_DETAILS_REQUIRED !in errors)
    }

    @Test
    fun correoInvalidoFalla() {
        val errors = validator.validate(base().copy(email = "no-es-un-correo"))
        assertTrue(AssessmentError.INVALID_EMAIL in errors)
    }

    @Test
    fun frecuenciaAltaVaciaLosObstaculosAlNormalizar() {
        val a = base().copy(activityFrequency = 6, obstacles = setOf(Obstacle.TIREDNESS))
        assertEquals(emptySet<Obstacle>(), a.normalized().obstacles)
    }

    @Test
    fun frecuenciaBajaConservaLosObstaculosAlNormalizar() {
        val a = base().copy(activityFrequency = 2, obstacles = setOf(Obstacle.LITTLE_TIME))
        assertEquals(setOf(Obstacle.LITTLE_TIME), a.normalized().obstacles)
    }
}
