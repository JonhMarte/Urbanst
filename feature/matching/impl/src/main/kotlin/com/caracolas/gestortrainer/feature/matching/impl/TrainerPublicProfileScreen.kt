package com.caracolas.gestortrainer.feature.matching.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService

private val HOW_IT_WORKS = listOf(
    "Envías tu solicitud contándole qué buscas.",
    "El trainer te manda una propuesta con precio y duración.",
    "Aceptas y pagas de forma segura para empezar.",
)

/** Perfil público del trainer visto desde el descubrimiento (carrusel / búsqueda). */
@Composable
fun TrainerPublicProfileScreen(
    trainerId: TrainerId,
    profileRepository: ProfileRepository,
    reviewService: ReviewService,
    onSendRequest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var profile by remember { mutableStateOf<TrainerProfile?>(null) }
    var videoPlaying by remember { mutableStateOf(false) }
    val reviews by reviewService.observe(trainerId).collectAsState(initial = null)

    LaunchedEffect(trainerId) {
        profileRepository.observeTrainer(trainerId).collect { profile = it }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SecondaryButton(text = "← Volver", onClick = onBack)

        val trainer = profile
        if (trainer == null) {
            Text("Cargando perfil…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (videoPlaying) "⏸" else "▶", style = MaterialTheme.typography.displaySmall)
            SecondaryButton(
                text = if (videoPlaying) "Pausar vídeo de presentación" else "Ver vídeo de presentación",
                onClick = { videoPlaying = !videoPlaying },
            )
        }

        Text(trainer.personal.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "${trainer.mainDiscipline} · ${"%.1f".format(reviews?.averageStars ?: 0.0)} ★ (${reviews?.count ?: 0}) · ${trainer.yearsAsTrainer} años",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (trainer.secondaryDisciplines.isNotEmpty() || trainer.mainDiscipline.isNotBlank()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf(trainer.mainDiscipline) + trainer.secondaryDisciplines).filter { it.isNotBlank() }.distinct().forEach { chip ->
                    Text(
                        chip,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (trainer.bio.isNotBlank()) {
            Text("Sobre mí", fontWeight = FontWeight.Bold)
            Text(trainer.bio)
        }

        Text("Cómo funciona", fontWeight = FontWeight.Bold)
        HOW_IT_WORKS.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${index + 1}.", fontWeight = FontWeight.Bold)
                Text(step)
            }
        }

        PrimaryButton(text = "Enviar solicitud", onClick = onSendRequest, modifier = Modifier.fillMaxWidth())
    }
}
