package com.caracolas.gestortrainer.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.onboarding.api.OnboardingService
import com.caracolas.gestortrainer.feature.onboarding.api.RegistrationData
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.PrivateBodyData
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidationError
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    role: Role,
    onboardingService: OnboardingService,
    modifier: Modifier = Modifier,
    onRegistered: (UserId) -> Unit = {},
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var nationality by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("España") }
    var ageText by rememberSaveable { mutableStateOf("") }
    var guardianConsent by rememberSaveable { mutableStateOf(false) }

    var mainDiscipline by rememberSaveable { mutableStateOf("") }
    var secondaryDisciplines by rememberSaveable { mutableStateOf("") }
    var yearsAsTrainerText by rememberSaveable { mutableStateOf("") }

    var weightText by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var bodyConsent by rememberSaveable { mutableStateOf(false) }

    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Crea tu cuenta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            if (role == Role.TRAINER) "Trainer" else "Alumno",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(fullName, { fullName = it }, label = { Text("Nombre y apellidos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(nationality, { nationality = it }, label = { Text("Nacionalidad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            region,
            { region = it },
            label = { Text(if (role == Role.TRAINER) "Región donde trabajas" else "Región donde vives") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(ageText, { ageText = it }, label = { Text("Edad") }, modifier = Modifier.fillMaxWidth())

        val age = ageText.toIntOrNull()
        if (age != null && age < 18) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(guardianConsent, { guardianConsent = it })
                Text("Tengo el consentimiento de mi padre, madre o tutor", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (role == Role.TRAINER) {
            OutlinedTextField(mainDiscipline, { mainDiscipline = it }, label = { Text("Disciplina principal") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                secondaryDisciplines,
                { secondaryDisciplines = it },
                label = { Text("Disciplinas secundarias (separadas por coma)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(yearsAsTrainerText, { yearsAsTrainerText = it }, label = { Text("Años como trainer") }, modifier = Modifier.fillMaxWidth())
        } else {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Privado: solo lo ve tu trainer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(weightText, { weightText = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(heightText, { heightText = it }, label = { Text("Altura (cm)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(bodyConsent, { bodyConsent = it })
                    Text(
                        "Acepto que mi trainer vea estos datos. Nunca aparecen en las clasificaciones.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        errors.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        PrimaryButton(
            text = if (submitting) "Creando cuenta…" else "Crear cuenta",
            onClick = {
                val personal = PersonalData(fullName, nationality, Region(region), age, guardianConsent)
                val body = if (role == Role.CLIENT && (weightText.isNotBlank() || heightText.isNotBlank())) {
                    PrivateBodyData(weightText.toDoubleOrNull() ?: 0.0, heightText.toIntOrNull() ?: 0, bodyConsent)
                } else {
                    null
                }
                val data = if (role == Role.TRAINER) {
                    RegistrationData.Trainer(
                        personal = personal,
                        mainDiscipline = mainDiscipline,
                        secondaryDisciplines = secondaryDisciplines.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        yearsAsTrainer = yearsAsTrainerText.toIntOrNull() ?: 0,
                    )
                } else {
                    RegistrationData.Client(personal = personal, body = body)
                }
                submitting = true
                scope.launch {
                    val result = onboardingService.register(data)
                    submitting = false
                    val exception = result.exceptionOrNull()
                    errors = when (exception) {
                        null -> emptyList()
                        is OnboardingValidationException -> exception.errors.map { it.toMessage() }
                        else -> listOf("No se pudo completar el registro. Inténtalo de nuevo.")
                    }
                    result.getOrNull()?.let(onRegistered)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun ProfileValidationError.toMessage(): String = when (this) {
    ProfileValidationError.FULL_NAME_REQUIRED -> "El nombre es obligatorio."
    ProfileValidationError.NATIONALITY_REQUIRED -> "La nacionalidad es obligatoria."
    ProfileValidationError.AGE_REQUIRED -> "Indica tu edad."
    ProfileValidationError.MINOR_REQUIRES_GUARDIAN_CONSENT -> "Los menores necesitan el consentimiento de un padre, madre o tutor."
    ProfileValidationError.BODY_DATA_REQUIRES_CONSENT -> "Acepta que tu trainer vea tus datos privados."
    ProfileValidationError.INVALID_WEIGHT -> "El peso no es válido."
    ProfileValidationError.INVALID_HEIGHT -> "La altura no es válida."
}
