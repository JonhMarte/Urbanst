package com.caracolas.gestortrainer.feature.muscles.impl

import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.feature.muscles.api.Muscle
import com.caracolas.gestortrainer.feature.muscles.api.MuscleCatalog
import com.caracolas.gestortrainer.feature.muscles.api.MuscleGroup
import javax.inject.Inject

class DefaultMuscleCatalog @Inject constructor() : MuscleCatalog {
    override fun all(): List<Muscle> = listOf(
        Muscle(MuscleId("shoulders"), "Hombros", MuscleGroup.UPPER),
        Muscle(MuscleId("chest"), "Pectoral", MuscleGroup.UPPER),
        Muscle(MuscleId("arms"), "Brazos", MuscleGroup.UPPER),
        Muscle(MuscleId("core"), "Core", MuscleGroup.CORE),
        Muscle(MuscleId("glutes"), "Glúteos", MuscleGroup.LOWER),
        Muscle(MuscleId("quads"), "Cuádriceps", MuscleGroup.LOWER),
        Muscle(MuscleId("hamstrings"), "Isquiotibiales", MuscleGroup.LOWER),
        Muscle(MuscleId("calves"), "Gemelos", MuscleGroup.LOWER),
    )
}
