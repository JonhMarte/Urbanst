package com.caracolas.gestortrainer.feature.activities.api

import com.caracolas.gestortrainer.core.model.ActivityId
import com.caracolas.gestortrainer.core.model.ExerciseMedia
import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.core.model.TrainerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class ActivityPreviewTest {

    @Test
    fun `la vista del alumno conserva todo menos el tiempo promedio y el trainer`() {
        val activity = Activity(
            id = ActivityId("a1"),
            trainerId = TrainerId("t1"),
            type = ActivityType.PHYSICAL,
            title = "Sentadilla con barra",
            description = "Baja hasta paralelo.",
            averageDuration = 45.minutes,
            muscles = listOf(MuscleId("quads"), MuscleId("glutes")),
            media = listOf(ExerciseMedia.Video("https://example.test/video.mp4", 60_000)),
        )

        val preview = activity.toClientPreview()

        assertEquals(activity.id, preview.id)
        assertEquals(activity.type, preview.type)
        assertEquals(activity.title, preview.title)
        assertEquals(activity.description, preview.description)
        assertEquals(activity.muscles, preview.muscles)
        assertEquals(activity.media, preview.media)
        // ActivityPreview no declara un campo de duración: no hay forma de que el alumno lo reciba.
    }
}
