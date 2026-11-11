package com.caracolas.gestortrainer.core.featureflags

/** Funcionalidades que existen en el código pero pueden estar apagadas. */
enum class Flag { NUTRITION, GROUP_CLASSES, VIEWER_3D, AR, SOCIAL_SHARE, REWARDS }

interface FeatureFlags {
    fun isEnabled(flag: Flag): Boolean
}

/** Interruptores fijos. En el MVP todos están apagados ("Próximamente"). */
class StaticFeatureFlags(private val enabled: Set<Flag> = emptySet()) : FeatureFlags {
    override fun isEnabled(flag: Flag): Boolean = flag in enabled
}
