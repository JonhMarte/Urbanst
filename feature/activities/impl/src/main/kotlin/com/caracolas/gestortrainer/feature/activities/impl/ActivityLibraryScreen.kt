package com.caracolas.gestortrainer.feature.activities.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.ComingSoonButton
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.ExerciseMedia
import com.caracolas.gestortrainer.core.model.LocalFile
import com.caracolas.gestortrainer.core.model.MediaKind
import com.caracolas.gestortrainer.core.model.MuscleId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.activities.api.ActivityDraft
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.activities.api.ActivityType
import com.caracolas.gestortrainer.feature.activities.api.ActivityValidator
import com.caracolas.gestortrainer.feature.activities.api.ValidationError
import com.caracolas.gestortrainer.feature.media.api.MediaUploader
import com.caracolas.gestortrainer.feature.media.api.MediaViewerRegistry
import com.caracolas.gestortrainer.feature.muscles.api.MuscleCatalog
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Biblioteca de actividades del trainer y el formulario ADD New Activity. */
@Composable
fun ActivityLibraryScreen(
    trainerId: TrainerId,
    muscleMapRenderer: MuscleMapRenderer,
    muscleCatalog: MuscleCatalog,
    activityRepository: ActivityRepository,
    activityValidator: ActivityValidator,
    mediaUploader: MediaUploader,
    mediaViewerRegistry: MediaViewerRegistry,
    modifier: Modifier = Modifier,
    extraActions: @Composable () -> Unit = {},
) {
    var showForm by remember { mutableStateOf(false) }

    if (showForm) {
        NewActivityForm(
            activityRepository = activityRepository,
            activityValidator = activityValidator,
            mediaUploader = mediaUploader,
            mediaViewerRegistry = mediaViewerRegistry,
            muscleCatalog = muscleCatalog,
            muscleMapRenderer = muscleMapRenderer,
            onDone = { showForm = false },
            modifier = modifier,
        )
        return
    }

    val activities by activityRepository.observeLibrary(trainerId).collectAsState(initial = emptyList())
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Actividades", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        PrimaryButton("+ ADD New Activity", onClick = { showForm = true }, modifier = Modifier.fillMaxWidth())
        extraActions()
        activities.forEach { activity ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(activity.title, fontWeight = FontWeight.Bold)
                Text(
                    activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NewActivityForm(
    activityRepository: ActivityRepository,
    activityValidator: ActivityValidator,
    mediaUploader: MediaUploader,
    mediaViewerRegistry: MediaViewerRegistry,
    muscleCatalog: MuscleCatalog,
    muscleMapRenderer: MuscleMapRenderer,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationMinutesText by remember { mutableStateOf("") }
    var selectedMuscles by remember { mutableStateOf(setOf<MuscleId>()) }
    var media by remember { mutableStateOf<List<ExerciseMedia>>(emptyList()) }
    var uploading by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<List<ValidationError>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Nueva actividad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Física",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            ComingSoonButton("Nutricional")
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Nombre del ejercicio") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = durationMinutesText,
            onValueChange = { durationMinutesText = it },
            label = { Text("Tiempo promedio (min)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "No lo ve el alumno. Se usa para la puntuación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Músculos que intervienen", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                muscleCatalog.all().forEach { muscle ->
                    val selected = muscle.id in selectedMuscles
                    if (selected) {
                        PrimaryButton(
                            text = muscle.name,
                            onClick = { selectedMuscles = selectedMuscles - muscle.id },
                        )
                    } else {
                        SecondaryButton(
                            text = muscle.name,
                            onClick = { selectedMuscles = selectedMuscles + muscle.id },
                        )
                    }
                }
            }
            muscleMapRenderer.Render(selected = selectedMuscles, modifier = Modifier.size(width = 96.dp, height = 176.dp))
        }

        Text("Video del instructor", fontWeight = FontWeight.Bold)
        SecondaryButton(
            text = when {
                uploading -> "Subiendo…"
                media.isNotEmpty() -> "Video añadido ✓"
                else -> "Subir o grabar video"
            },
            onClick = {
                uploading = true
                scope.launch {
                    // Sin selector de archivos real todavía: se simula con un video de prueba público.
                    val result = mediaUploader.upload(
                        LocalFile(uri = "https://www.w3schools.com/html/mov_bbb.mp4", mimeType = "video/mp4"),
                        MediaKind.EXERCISE_VIDEO,
                    )
                    uploading = false
                    result.onSuccess { remote ->
                        media = listOf(ExerciseMedia.Video(url = remote.url, durationMs = 0))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        media.firstOrNull()?.let { video ->
            mediaViewerRegistry.viewerFor(video)?.Content(video, Modifier.fillMaxWidth().height(200.dp))
        }

        errors.forEach { Text(it.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(text = "Cancelar", onClick = onDone, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Guardar actividad",
                onClick = {
                    val draft = ActivityDraft(
                        type = ActivityType.PHYSICAL,
                        title = title,
                        description = description,
                        averageDuration = parseMinutes(durationMinutesText),
                        muscles = selectedMuscles.toList(),
                        media = media,
                    )
                    val validationErrors = activityValidator.validate(draft)
                    errors = validationErrors
                    if (validationErrors.isEmpty()) {
                        scope.launch {
                            activityRepository.save(draft)
                            onDone()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun parseMinutes(text: String): Duration = text.toLongOrNull()?.minutes ?: Duration.ZERO

private fun ValidationError.toMessage(): String = when (this) {
    ValidationError.TITLE_REQUIRED -> "El nombre del ejercicio es obligatorio."
    ValidationError.DESCRIPTION_REQUIRED -> "La descripción es obligatoria."
    ValidationError.DURATION_REQUIRED -> "Indica el tiempo promedio."
    ValidationError.MUSCLES_REQUIRED_FOR_PHYSICAL -> "Selecciona al menos un músculo."
    ValidationError.MUSCLES_NOT_ALLOWED_FOR_NUTRITION -> "Las actividades nutricionales no llevan músculos."
}
