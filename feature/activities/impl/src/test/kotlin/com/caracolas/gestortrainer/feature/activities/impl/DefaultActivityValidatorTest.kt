package com.caracolas.gestortrainer.feature.activities.impl

import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.feature.activities.api.ActivityDraft
import com.caracolas.gestortrainer.feature.activities.api.ActivityType
import com.caracolas.gestortrainer.feature.activities.api.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class DefaultActivityValidatorTest {
    private val validator = DefaultActivityValidator()

    @Test
    fun actividadFisicaCompletaEsValida() {
        val draft = ActivityDraft(
            type = ActivityType.PHYSICAL,
            title = "Sentadilla con barra",
            description = "Espalda neutra, baja hasta paralelo.",
            averageDuration = 45.minutes,
            muscles = listOf(MuscleId("quads")),
        )
        assertTrue(validator.validate(draft).isEmpty())
    }

    @Test
    fun actividadFisicaSinMusculosNoEsValida() {
        val draft = ActivityDraft(title = "X", description = "Y", averageDuration = 10.minutes)
        assertEquals(listOf(ValidationError.MUSCLES_REQUIRED_FOR_PHYSICAL), validator.validate(draft))
    }
}
