package com.caracolas.gestortrainer.feature.onboarding.impl

import com.caracolas.gestortrainer.core.model.Role
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PublicIdFactoryTest {

    @Test
    fun unTrainerRecibeUnIdConPrefijoTR() {
        val id = PublicIdFactory.create(Role.TRAINER, Random(0))
        assertTrue(id.value.matches(Regex("TR-\\d{5}")))
    }

    @Test
    fun unClienteRecibeUnIdConPrefijoAL() {
        val id = PublicIdFactory.create(Role.CLIENT, Random(0))
        assertTrue(id.value.matches(Regex("AL-\\d{5}")))
    }
}
