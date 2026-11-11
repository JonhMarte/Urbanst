package com.caracolas.gestortrainer.feature.reviews.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.reviews.api.Review
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import kotlinx.coroutines.launch

/**
 * Valoraciones de un trainer: media, nº de valoraciones y comentarios recientes.
 * Si [clientId] no es nulo, el que mira es un alumno vinculado y puede valorar.
 */
@Composable
fun ReviewsScreen(
    trainerId: TrainerId,
    reviewService: ReviewService,
    clientId: ClientId? = null,
    modifier: Modifier = Modifier,
) {
    val summary by reviewService.observe(trainerId).collectAsState(initial = null)
    var stars by rememberSaveable { mutableStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Valoraciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val current = summary
        if (current == null || current.count == 0) {
            Text("Todavía sin valoraciones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                "${"%.1f".format(current.averageStars)} ★ · ${current.count} valoraciones",
                fontWeight = FontWeight.Bold,
            )
        }

        if (clientId != null) {
            Text("Valora a tu trainer", fontWeight = FontWeight.Bold)
            StarPicker(stars = stars, onChange = { stars = it })
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text("Comentario (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryButton(
                text = "Enviar valoración",
                onClick = {
                    if (stars <= 0) {
                        error = "Elige de 1 a 5 estrellas."
                    } else {
                        scope.launch {
                            reviewService.rate(clientId, trainerId, stars, comment.ifBlank { null })
                                .onSuccess { stars = 0; comment = ""; error = null }
                                .onFailure { error = it.message }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        current?.recent?.forEach { review -> ReviewRow(review) }
    }
}

@Composable
private fun StarPicker(stars: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { value ->
            Text(
                text = if (value <= stars) "★" else "☆",
                fontSize = 28.sp,
                modifier = Modifier.clickable { onChange(value) },
            )
        }
    }
}

@Composable
private fun ReviewRow(review: Review) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("${"★".repeat(review.stars)}${"☆".repeat(5 - review.stars)}", fontWeight = FontWeight.Bold)
        review.comment?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
