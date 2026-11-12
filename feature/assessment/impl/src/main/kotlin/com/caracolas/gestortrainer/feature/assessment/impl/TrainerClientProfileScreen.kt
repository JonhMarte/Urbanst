package com.caracolas.gestortrainer.feature.assessment.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentRepository
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository

/** Ficha del alumno vista por el trainer: solo lectura. */
@Composable
fun TrainerClientProfileScreen(
    clientId: ClientId,
    assessmentRepository: AssessmentRepository,
    profileRepository: ProfileRepository,
    muscleMapRenderer: MuscleMapRenderer,
    onOpenMessage: () -> Unit,
    onOpenCalendar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile by profileRepository.observeClient(clientId).collectAsState(initial = null)
    val assessment by assessmentRepository.observe(clientId).collectAsState(initial = null)

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)
        Text(profile?.personal?.fullName ?: clientId.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(text = "Mensaje", onClick = onOpenMessage, modifier = Modifier.weight(1f))
            SecondaryButton(text = "Calendario", onClick = onOpenCalendar, modifier = Modifier.weight(1f))
        }

        val current = assessment
        if (current == null) {
            Text("Todavía no completó su cuestionario.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        Text("Objetivos", fontWeight = FontWeight.Bold)
        Text(current.goals.joinToString(", ") { it.label() }.ifBlank { "—" })

        Text("Zonas del cuerpo", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(current.bodyAreas.joinToString(", ") { it.label() }.ifBlank { "—" })
            }
            muscleMapRenderer.Render(
                selected = current.bodyAreas.map { it.toMuscleId() }.toSet(),
                modifier = Modifier.size(width = 72.dp, height = 132.dp),
            )
        }

        Text("Frecuencia de actividad", fontWeight = FontWeight.Bold)
        Text("${current.activityFrequency} / 10")
        if (current.activityFrequency < 4 && current.obstacles.isNotEmpty()) {
            Text("Se lo dificulta: ${current.obstacles.joinToString(", ") { it.label() }}")
        }

        current.dailyTime?.let { Text("Tiempo diario: ${it.label()}") }
        current.daysPerWeek?.let { Text("Días por semana: $it") }

        if (current.psychomotorIssue) {
            Text("Problema psicomotriz (dato privado, con consentimiento)", fontWeight = FontWeight.Bold)
            Text(current.psychomotorDetails.orEmpty())
        }

        Text("Correo: ${current.email}")
    }
}
