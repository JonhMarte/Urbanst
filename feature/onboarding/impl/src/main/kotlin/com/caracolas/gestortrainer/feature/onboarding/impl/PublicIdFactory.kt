package com.caracolas.gestortrainer.feature.onboarding.impl

import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Role
import kotlin.random.Random

/** IDs públicos para vincularse (ej. TR-48213, AL-30917). */
object PublicIdFactory {
    fun create(role: Role, random: Random = Random): PublicId {
        val prefix = if (role == Role.TRAINER) "TR" else "AL"
        val number = random.nextInt(10_000, 100_000)
        return PublicId("$prefix-$number")
    }
}
