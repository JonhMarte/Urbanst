package com.caracolas.gestortrainer.feature.assessment.impl

import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.feature.assessment.api.BodyArea
import com.caracolas.gestortrainer.feature.assessment.api.DailyTime
import com.caracolas.gestortrainer.feature.assessment.api.FitnessGoal
import com.caracolas.gestortrainer.feature.assessment.api.Obstacle

/** BodyArea → MuscleId para sincronizar cuerpo y botones. La silueta es frontal: Espalda solo se elige con su botón. */
internal fun BodyArea.toMuscleId() = MuscleId(
    when (this) {
        BodyArea.SHOULDERS -> "shoulders"
        BodyArea.CHEST -> "chest"
        BodyArea.ARMS -> "arms"
        BodyArea.CORE -> "core"
        BodyArea.BACK -> "back"
        BodyArea.GLUTES -> "glutes"
        BodyArea.LEGS -> "quads"
        BodyArea.CALVES -> "calves"
    },
)

internal fun MuscleId.toBodyArea(): BodyArea? = when (value) {
    "shoulders" -> BodyArea.SHOULDERS
    "chest" -> BodyArea.CHEST
    "arms" -> BodyArea.ARMS
    "core" -> BodyArea.CORE
    "back" -> BodyArea.BACK
    "glutes" -> BodyArea.GLUTES
    "quads" -> BodyArea.LEGS
    "calves" -> BodyArea.CALVES
    else -> null
}

internal fun FitnessGoal.label() = when (this) {
    FitnessGoal.STRENGTH -> "Fuerza"
    FitnessGoal.FLEXIBILITY -> "Flexibilidad"
    FitnessGoal.TONING -> "Tonificación"
    FitnessGoal.WEIGHT_LOSS -> "Bajar de peso"
    FitnessGoal.HYPERTROPHY -> "Hipertrofia (masa muscular)"
    FitnessGoal.ENDURANCE -> "Resistencia"
}

internal fun BodyArea.label() = when (this) {
    BodyArea.SHOULDERS -> "Hombros"
    BodyArea.CHEST -> "Pecho"
    BodyArea.ARMS -> "Brazos"
    BodyArea.CORE -> "Abdomen"
    BodyArea.BACK -> "Espalda"
    BodyArea.GLUTES -> "Glúteos"
    BodyArea.LEGS -> "Piernas"
    BodyArea.CALVES -> "Gemelos"
}

internal fun Obstacle.label() = when (this) {
    Obstacle.TIREDNESS -> "Cansancio"
    Obstacle.LACK_OF_MOTIVATION -> "Falta de motivación"
    Obstacle.TOO_MUCH_WORK -> "Mucho trabajo"
    Obstacle.LITTLE_TIME -> "Poco tiempo"
}

internal fun DailyTime.label() = when (this) {
    DailyTime.MIN_15 -> "15 min"
    DailyTime.MIN_30 -> "30 min"
    DailyTime.HOUR_1 -> "1 hora"
    DailyTime.HOURS_2 -> "2 horas"
}
