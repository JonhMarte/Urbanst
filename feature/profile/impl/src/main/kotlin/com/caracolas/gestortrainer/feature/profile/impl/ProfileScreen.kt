package com.caracolas.gestortrainer.feature.profile.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.caracolas.gestortrainer.core.auth.Session
import com.caracolas.gestortrainer.core.auth.SessionRepository
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.PrivateBodyData
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidationError
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    session: Session,
    profileRepository: ProfileRepository,
    profileValidator: ProfileValidator,
    sessionRepository: SessionRepository,
    modifier: Modifier = Modifier,
) {
    when (session.role) {
        Role.TRAINER -> TrainerProfileForm(session, profileRepository, sessionRepository, modifier)
        Role.CLIENT -> ClientProfileForm(session, profileRepository, profileValidator, sessionRepository, modifier)
    }
}

@Composable
private fun TrainerProfileForm(
    session: Session,
    repository: ProfileRepository,
    sessionRepository: SessionRepository,
    modifier: Modifier,
) {
    val trainerId = remember(session) { TrainerId(session.userId.value) }
    val profile by repository.observeTrainer(trainerId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var fullName by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(profile) {
        profile?.let {
            fullName = it.personal.fullName
            bio = it.bio
        }
    }

    val current = profile
    FormScaffold(modifier, title = "Datos personales", subtitle = "Tu ID: ${session.publicId.value}") {
        OutlinedTextField(fullName, { fullName = it }, label = { Text("Nombre y apellidos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(bio, { bio = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = "Guardar cambios",
            onClick = {
                val base = current ?: TrainerProfile(
                    userId = session.userId,
                    publicId = session.publicId,
                    personal = PersonalData("", "", Region("ES"), null),
                    mainDiscipline = "",
                    secondaryDisciplines = emptyList(),
                    yearsAsTrainer = 0,
                    bio = "",
                    photoUrl = null,
                )
                scope.launch {
                    repository.saveTrainer(base.copy(personal = base.personal.copy(fullName = fullName), bio = bio))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = "Cerrar sesión",
            onClick = { scope.launch { sessionRepository.signOut() } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ClientProfileForm(
    session: Session,
    repository: ProfileRepository,
    validator: ProfileValidator,
    sessionRepository: SessionRepository,
    modifier: Modifier,
) {
    val clientId = remember(session) { ClientId(session.userId.value) }
    val profile by repository.observeClient(clientId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var fullName by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var consentGiven by rememberSaveable { mutableStateOf(false) }
    var errors by remember { mutableStateOf<List<ProfileValidationError>>(emptyList()) }

    LaunchedEffect(profile) {
        profile?.let {
            fullName = it.personal.fullName
            weightText = it.body?.weightKg?.toString().orEmpty()
            heightText = it.body?.heightCm?.toString().orEmpty()
            consentGiven = it.body?.consentGiven ?: false
        }
    }

    val current = profile
    FormScaffold(modifier, title = "Datos personales", subtitle = "Tu ID: ${session.publicId.value}") {
        OutlinedTextField(fullName, { fullName = it }, label = { Text("Nombre y apellidos") }, modifier = Modifier.fillMaxWidth())

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
                Checkbox(consentGiven, { consentGiven = it })
                Text(
                    "Acepto que mi trainer vea estos datos. Nunca aparecen en las clasificaciones.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        errors.forEach { Text(it.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        PrimaryButton(
            text = "Guardar cambios",
            onClick = {
                val body = if (weightText.isNotBlank() || heightText.isNotBlank()) {
                    PrivateBodyData(weightText.toDoubleOrNull() ?: 0.0, heightText.toIntOrNull() ?: 0, consentGiven)
                } else {
                    null
                }
                val bodyErrors = body?.let { validator.validate(it) }.orEmpty()
                errors = bodyErrors
                if (bodyErrors.isEmpty()) {
                    val base = current ?: ClientProfile(
                        userId = session.userId,
                        publicId = session.publicId,
                        personal = PersonalData("", "", Region("ES"), null),
                        body = null,
                        photoUrl = null,
                    )
                    scope.launch {
                        repository.saveClient(base.copy(personal = base.personal.copy(fullName = fullName), body = body))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = "Cerrar sesión",
            onClick = { scope.launch { sessionRepository.signOut() } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FormScaffold(
    modifier: Modifier,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
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
