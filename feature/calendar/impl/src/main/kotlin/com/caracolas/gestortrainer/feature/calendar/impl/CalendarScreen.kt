package com.caracolas.gestortrainer.feature.calendar.impl

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.DateRange
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.activities.api.Activity
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.calendar.api.Assignment
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.clients.api.TrainerNotesService
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Calendario del alumno: el trainer le asigna actividades de su biblioteca día a día. */
@Composable
fun CalendarScreen(
    trainerId: TrainerId,
    clientId: ClientId,
    assignmentService: AssignmentService,
    activityRepository: ActivityRepository,
    profileRepository: ProfileRepository,
    trainerNotesService: TrainerNotesService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clientProfile by profileRepository.observeClient(clientId).collectAsState(initial = null)
    val library by activityRepository.observeLibrary(trainerId).collectAsState(initial = emptyList())
    val notes by trainerNotesService.observeNotes(clientId).collectAsState(initial = emptyList())
    var noteText by remember { mutableStateOf("") }
    val weekStart = remember { LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val week = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val assignments by assignmentService
        .observeCalendar(clientId, DateRange(weekStart, weekStart.plusDays(6)))
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text(
            clientProfile?.personal?.fullName ?: clientId.value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text("Calendario", fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            week.forEach { date ->
                val count = assignments.count { it.date == date }
                val selected = date == selectedDate
                val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { selectedDate = date }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")), color = textColor)
                    Text(date.dayOfMonth.toString(), color = textColor, fontWeight = FontWeight.Bold)
                    Text(if (count > 0) "$count act." else "—", color = textColor)
                }
            }
        }

        val dayAssignments = assignments.filter { it.date == selectedDate }
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Actividades asignadas", fontWeight = FontWeight.Bold)
            if (dayAssignments.isEmpty()) {
                Text("Arrastra aquí una actividad", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            dayAssignments.forEach { assignment ->
                AssignedActivityRow(
                    assignment = assignment,
                    library = library,
                    onRemove = { scope.launch { assignmentService.unassign(assignment.id) } },
                )
            }
        }

        Text("Mis actividades", fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            library.forEach { activity ->
                SecondaryButton(
                    text = "+ ${activity.title}",
                    onClick = { scope.launch { assignmentService.assign(activity.id, clientId, selectedDate) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Notas para ${clientProfile?.personal?.fullName ?: "tu alumno"}", fontWeight = FontWeight.Bold)
            notes.forEach { note ->
                Text(note.text, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Escribe una nota…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Enviar",
                    onClick = {
                        val text = noteText
                        scope.launch {
                            if (trainerNotesService.addNote(trainerId, clientId, text).isSuccess) noteText = ""
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AssignedActivityRow(assignment: Assignment, library: List<Activity>, onRemove: () -> Unit) {
    val title = library.find { it.id == assignment.activityId }?.title ?: assignment.activityId.value
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title)
        PrimaryButton(text = "Quitar", onClick = onRemove)
    }
}
