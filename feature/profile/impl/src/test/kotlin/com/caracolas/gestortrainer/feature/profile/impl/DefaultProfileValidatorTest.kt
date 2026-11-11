package com.caracolas.gestortrainer.feature.profile.impl

import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.PrivateBodyData
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultProfileValidatorTest {
    private val validator = DefaultProfileValidator()

    private fun adult(name: String = "Lucía Gómez", age: Int? = 29) =
        PersonalData(fullName = name, nationality = "Española", region = Region("ES"), age = age)

    @Test
    fun datosPersonalesCompletosSonValidos() {
        assertTrue(validator.validate(adult()).isEmpty())
    }

    @Test
    fun nombreVacioNoEsValido() {
        assertEquals(listOf(ProfileValidationError.FULL_NAME_REQUIRED), validator.validate(adult(name = "")))
    }

    @Test
    fun sinEdadNoEsValido() {
        assertEquals(listOf(ProfileValidationError.AGE_REQUIRED), validator.validate(adult(age = null)))
    }

    @Test
    fun menorDeEdadSinConsentimientoNoEsValido() {
        val menor = adult(age = 16)
        assertEquals(listOf(ProfileValidationError.MINOR_REQUIRES_GUARDIAN_CONSENT), validator.validate(menor))
    }

    @Test
    fun menorDeEdadConConsentimientoEsValido() {
        val menor = adult(age = 16).copy(guardianConsentGiven = true)
        assertTrue(validator.validate(menor).isEmpty())
    }

    @Test
    fun datosCorporalesSinConsentimientoNoSonValidos() {
        val body = PrivateBodyData(weightKg = 61.0, heightCm = 165, consentGiven = false)
        assertEquals(listOf(ProfileValidationError.BODY_DATA_REQUIRES_CONSENT), validator.validate(body))
    }

    @Test
    fun datosCorporalesConValoresInvalidosAcumulaErrores() {
        val body = PrivateBodyData(weightKg = 0.0, heightCm = -5, consentGiven = true)
        assertEquals(
            listOf(ProfileValidationError.INVALID_WEIGHT, ProfileValidationError.INVALID_HEIGHT),
            validator.validate(body),
        )
    }

    @Test
    fun datosCorporalesCompletosSonValidos() {
        val body = PrivateBodyData(weightKg = 61.0, heightCm = 165, consentGiven = true)
        assertTrue(validator.validate(body).isEmpty())
    }
}
