package com.caracolas.gestortrainer.feature.plan.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.LocalFile
import com.caracolas.gestortrainer.core.model.MediaKind
import com.caracolas.gestortrainer.feature.activities.api.Activity
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.calendar.api.Assignment
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import com.caracolas.gestortrainer.feature.clients.api.TrainerNotesService
import com.caracolas.gestortrainer.feature.media.api.MediaUploader
import com.caracolas.gestortrainer.feature.plan.api.WeeklyPlanService
import com.caracolas.gestortrainer.feature.tracking.api.ActivityTracker
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Plan semanal del alumno: sus actividades asignadas por día, con Activar / Finalizar. */
@Composable
fun WeeklyPlanScreen(
    clientId: ClientId,
    weeklyPlanService: WeeklyPlanService,
    activityTracker: ActivityTracker,
    activityRepository: ActivityRepository,
    mediaUploader: MediaUploader,
    trainerNotesService: TrainerNotesService,
    modifier: Modifier = Modifier,
) {
    val weekStart = remember { LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val plan by weeklyPlanService.observeWeek(clientId, weekStart).collectAsState(initial = null)
    val notes by trainerNotesService.observeNotes(clientId).collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Mi plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        val days = plan?.days?.keys?.sorted().orEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            days.forEach { date ->
                val count = plan?.days?.get(date)?.size ?: 0
                val selected = date == selectedDate
                val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { selectedDate = date }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")), color = textColor)
                    Text(date.dayOfMonth.toString(), color = textColor, fontWeight = FontWeight.Bold)
                    Text(if (count > 0) "$count" else "—", color = textColor)
                }
            }
        }

        val todayAssignments = plan?.days?.get(selectedDate).orEmpty()
        Text("Actividades del día", fontWeight = FontWeight.Bold)
        if (todayAssignments.isEmpty()) {
            Text("Sin actividades hoy", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        todayAssignments.forEach { assignment ->
            AssignmentCard(
                assignment = assignment,
                activityRepository = activityRepository,
                onActivate = { scope.launch { activityTracker.start(assignment.id) } },
                onFinishWithPhoto = {
                    scope.launch {
                        val sessionId = activityTracker.activeSessionFor(assignment.id) ?: return@launch
                        val photo = mediaUploader.upload(
                            LocalFile(uri = "content://evidence/${assignment.id.value}", mimeType = "image/jpeg"),
                            MediaKind.ACTIVITY_PHOTO,
                        ).getOrNull()
                        photo?.let { activityTracker.attachPhoto(sessionId, it) }
                        activityTracker.finish(sessionId)
                    }
                },
            )
        }

        notes.firstOrNull()?.let { note ->
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Nota de tu trainer",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(note.text, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    assignment: Assignment,
    activityRepository: ActivityRepository,
    onActivate: () -> Unit,
    onFinishWithPhoto: () -> Unit,
) {
    val activity by produceState<Activity?>(initialValue = null, assignment.activityId) {
        value = activityRepository.get(assignment.activityId).getOrNull()
    }
    var finishing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(activity?.title ?: assignment.activityId.value, fontWeight = FontWeight.Bold)
        Text(assignment.status.toLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (assignment.status) {
            AssignmentStatus.DONE -> PrimaryButton(text = "Completada ✓", onClick = {}, modifier = Modifier.fillMaxWidth())
            AssignmentStatus.PENDING -> PrimaryButton(text = "Activar", onClick = onActivate, modifier = Modifier.fillMaxWidth())
            AssignmentStatus.IN_PROGRESS -> PrimaryButton(
                text = if (finishing) "Subiendo foto…" else "Subir foto y finalizar",
                onClick = { finishing = true; onFinishWithPhoto() },
                modifier = Modifier.fillMaxWidth(),
            )
            AssignmentStatus.MISSED -> Text("No completada", color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun AssignmentStatus.toLabel(): String = when (this) {
    AssignmentStatus.PENDING -> "Pendiente"
    AssignmentStatus.IN_PROGRESS -> "En curso…"
    AssignmentStatus.DONE -> "Completada"
    AssignmentStatus.MISSED -> "No completada"
}
