package com.caracolas.gestortrainer.feature.assessment.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentError
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentRepository
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentValidator
import com.caracolas.gestortrainer.feature.assessment.api.BodyArea
import com.caracolas.gestortrainer.feature.assessment.api.ClientAssessment
import com.caracolas.gestortrainer.feature.assessment.api.DailyTime
import com.caracolas.gestortrainer.feature.assessment.api.FitnessGoal
import com.caracolas.gestortrainer.feature.assessment.api.Obstacle
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import kotlinx.coroutines.launch

private fun AssessmentError.toMessage(): String = when (this) {
    AssessmentError.ACTIVITY_FREQUENCY_OUT_OF_RANGE -> "La frecuencia debe estar entre 0 y 10."
    AssessmentError.DAYS_PER_WEEK_OUT_OF_RANGE -> "Los días por semana deben estar entre 1 y 6."
    AssessmentError.PSYCHOMOTOR_DETAILS_REQUIRED -> "Cuéntanos brevemente el problema psicomotriz."
    AssessmentError.PSYCHOMOTOR_DETAILS_TOO_LONG -> "Máximo 500 caracteres."
    AssessmentError.INVALID_EMAIL -> "El correo no es válido."
}

/**
 * Cuestionario del alumno en 2 pasos (objetivo/zonas y frecuencia/hábitos/salud).
 * [onSkip] solo se ofrece en el onboarding («Completar después»); en modo edición es null.
 */
@Composable
fun AssessmentQuestionnaireScreen(
    clientId: ClientId,
    accountEmail: String,
    assessmentRepository: AssessmentRepository,
    assessmentValidator: AssessmentValidator,
    muscleMapRenderer: MuscleMapRenderer,
    onDone: () -> Unit,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(1) }
    var goals by remember { mutableStateOf(setOf<FitnessGoal>()) }
    var bodyAreas by remember { mutableStateOf(setOf<BodyArea>()) }
    var frequencyText by remember { mutableStateOf("0") }
    var obstacles by remember { mutableStateOf(setOf<Obstacle>()) }
    var dailyTime by remember { mutableStateOf<DailyTime?>(null) }
    var daysPerWeekText by remember { mutableStateOf("") }
    var psychomotorIssue by remember { mutableStateOf(false) }
    var psychomotorConsent by remember { mutableStateOf(false) }
    var psychomotorDetails by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(accountEmail) }
    var errors by remember { mutableStateOf<List<AssessmentError>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId) {
        assessmentRepository.observe(clientId).collect { existing ->
            if (existing != null && !loaded) {
                goals = existing.goals
                bodyAreas = existing.bodyAreas
                frequencyText = existing.activityFrequency.toString()
                obstacles = existing.obstacles
                dailyTime = existing.dailyTime
                daysPerWeekText = existing.daysPerWeek?.toString().orEmpty()
                psychomotorIssue = existing.psychomotorIssue
                psychomotorConsent = existing.psychomotorIssue
                psychomotorDetails = existing.psychomotorDetails.orEmpty()
                email = existing.email.ifBlank { accountEmail }
            }
            loaded = true
        }
    }

    fun buildAssessment() = ClientAssessment(
        clientId = clientId,
        goals = goals,
        bodyAreas = bodyAreas,
        activityFrequency = frequencyText.toIntOrNull() ?: 0,
        obstacles = obstacles,
        dailyTime = dailyTime,
        daysPerWeek = daysPerWeekText.toIntOrNull(),
        psychomotorIssue = psychomotorIssue && psychomotorConsent,
        psychomotorDetails = psychomotorDetails.ifBlank { null },
        email = email.trim(),
    )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Cuestionario · Paso $step de 2", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (step == 1) {
            Text("¿Qué quieres mejorar?", fontWeight = FontWeight.Bold)
            ToggleChips(FitnessGoal.entries, goals, { goals = it }, FitnessGoal::label)

            Text("Zonas del cuerpo", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BodyArea.entries.forEach { area ->
                        val selected = area in bodyAreas
                        val toggle = { bodyAreas = if (selected) bodyAreas - area else bodyAreas + area }
                        if (selected) {
                            PrimaryButton(text = area.label(), onClick = toggle, modifier = Modifier.fillMaxWidth())
                        } else {
                            SecondaryButton(text = area.label(), onClick = toggle, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                muscleMapRenderer.RenderSelectable(
                    selected = bodyAreas.map { it.toMuscleId() }.toSet(),
                    onToggle = { muscleId ->
                        muscleId.toBodyArea()?.let { area ->
                            bodyAreas = if (area in bodyAreas) bodyAreas - area else bodyAreas + area
                        }
                    },
                    modifier = Modifier.size(width = 96.dp, height = 176.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onSkip != null) {
                    SecondaryButton(text = "Completar después", onClick = onSkip, modifier = Modifier.weight(1f))
                }
                PrimaryButton(text = "Siguiente", onClick = { step = 2 }, modifier = Modifier.weight(1f))
            }
        } else {
            OutlinedTextField(
                value = frequencyText,
                onValueChange = { frequencyText = it.filter(Char::isDigit) },
                label = { Text("Frecuencia de actividad física (0-10)") },
                modifier = Modifier.fillMaxWidth(),
            )

            val frequency = frequencyText.toIntOrNull() ?: 0
            if (frequency < 4) {
                Text("¿Qué te lo dificulta?", fontWeight = FontWeight.Bold)
                ToggleChips(Obstacle.entries, obstacles, { obstacles = it }, Obstacle::label)
            }

            Text("Tiempo diario disponible", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyTime.entries.forEach { time ->
                    if (dailyTime == time) {
                        PrimaryButton(text = time.label(), onClick = { dailyTime = time })
                    } else {
                        SecondaryButton(text = time.label(), onClick = { dailyTime = time })
                    }
                }
            }

            OutlinedTextField(
                value = daysPerWeekText,
                onValueChange = { daysPerWeekText = it.filter(Char::isDigit) },
                label = { Text("Días por semana (1-6)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(psychomotorIssue, { psychomotorIssue = it; if (!it) psychomotorConsent = false })
                Text("Tengo un problema psicomotriz", style = MaterialTheme.typography.bodyMedium)
            }
            if (psychomotorIssue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(psychomotorConsent, { psychomotorConsent = it })
                    Text(
                        "Doy mi consentimiento explícito para guardar este dato de salud. Solo lo ve mi trainer vinculado, nunca en clasificaciones.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = psychomotorDetails,
                    onValueChange = { if (it.length <= 500) psychomotorDetails = it },
                    label = { Text("Cuéntanos brevemente") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())

            errors.forEach { Text(it.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Atrás", onClick = { step = 1 }, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Guardar",
                    onClick = {
                        val assessment = buildAssessment()
                        val validationErrors = assessmentValidator.validate(assessment)
                        errors = validationErrors
                        if (validationErrors.isEmpty()) {
                            scope.launch {
                                assessmentRepository.save(assessment)
                                onDone()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun <T> ToggleChips(all: List<T>, selected: Set<T>, onChange: (Set<T>) -> Unit, label: (T) -> String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        all.forEach { option ->
            val isSelected = option in selected
            val toggle = { onChange(if (isSelected) selected - option else selected + option) }
            if (isSelected) {
                PrimaryButton(text = label(option), onClick = toggle)
            } else {
                SecondaryButton(text = label(option), onClick = toggle)
            }
        }
    }
}
