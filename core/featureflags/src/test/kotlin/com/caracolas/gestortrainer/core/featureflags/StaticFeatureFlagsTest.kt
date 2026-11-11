package com.caracolas.gestortrainer.core.featureflags

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticFeatureFlagsTest {
    @Test
    fun `en el MVP nutricion y grupales estan apagados`() {
        val flags = StaticFeatureFlags()
        assertFalse(flags.isEnabled(Flag.NUTRITION))
        assertFalse(flags.isEnabled(Flag.GROUP_CLASSES))
        assertFalse(flags.isEnabled(Flag.VIEWER_3D))
    }

    @Test
    fun `un flag encendido se reporta como activo`() {
        assertTrue(StaticFeatureFlags(setOf(Flag.NUTRITION)).isEnabled(Flag.NUTRITION))
    }
}
