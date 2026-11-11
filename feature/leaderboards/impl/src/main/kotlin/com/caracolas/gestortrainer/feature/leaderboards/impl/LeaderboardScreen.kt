package com.caracolas.gestortrainer.feature.leaderboards.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caracolas.gestortrainer.core.designsystem.PrimaryButton
import com.caracolas.gestortrainer.core.designsystem.SecondaryButton
import com.caracolas.gestortrainer.core.model.PageRequest
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardScope
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardType
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardEntry
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardService

private fun BoardType.label(): String = when (this) {
    BoardType.MY_CLIENTS -> "Mis alumnos"
    BoardType.GLOBAL_USERS -> "Alumnos globales"
    BoardType.GLOBAL_TRAINERS -> "Trainers globales"
}

/**
 * Clasificaciones: alumnos de un trainer, alumnos globales y trainers globales.
 * [viewerTrainerId] solo se informa cuando quien mira es trainer, para ofrecer "Mis alumnos".
 */
@Composable
fun LeaderboardScreen(
    viewerUserId: UserId,
    viewerTrainerId: TrainerId? = null,
    leaderboardService: LeaderboardService,
    modifier: Modifier = Modifier,
) {
    val tabs = remember(viewerTrainerId) {
        if (viewerTrainerId != null) {
            listOf(BoardType.MY_CLIENTS, BoardType.GLOBAL_USERS, BoardType.GLOBAL_TRAINERS)
        } else {
            listOf(BoardType.GLOBAL_USERS, BoardType.GLOBAL_TRAINERS)
        }
    }
    var selected by rememberSaveable { mutableStateOf(tabs.first()) }

    val scope = when (selected) {
        BoardType.MY_CLIENTS -> viewerTrainerId?.let { BoardScope.MyTrainer(it) } ?: BoardScope.Global
        BoardType.GLOBAL_USERS, BoardType.GLOBAL_TRAINERS -> BoardScope.Global
    }
    val page by leaderboardService.observe(selected, scope, PageRequest())
        .collectAsState(initial = null)

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Clasificaciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { type ->
                if (type == selected) {
                    PrimaryButton(text = type.label(), onClick = { selected = type })
                } else {
                    SecondaryButton(text = type.label(), onClick = { selected = type })
                }
            }
        }

        val entries = page?.entries.orEmpty()
        if (page != null && entries.isEmpty()) {
            Text("Todavía no hay datos en esta tabla.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.forEach { entry -> LeaderboardRow(entry, isMe = entry.userId == viewerUserId) }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, isMe: Boolean) {
    val background = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${entry.position}. ${entry.displayName}", fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal)
        Text(entry.score.toInt().toString(), fontWeight = FontWeight.Bold)
    }
}
