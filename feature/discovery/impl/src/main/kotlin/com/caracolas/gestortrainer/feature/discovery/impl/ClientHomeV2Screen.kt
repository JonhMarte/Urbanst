package com.caracolas.gestortrainer.feature.discovery.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.caracolas.gestortrainer.core.model.PageRequest
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentStatus
import com.caracolas.gestortrainer.feature.discovery.api.DiscoveryService
import com.caracolas.gestortrainer.feature.discovery.api.PopularCategory
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardScope
import com.caracolas.gestortrainer.feature.leaderboards.api.BoardType
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardService
import com.caracolas.gestortrainer.feature.messaging.api.MessagingService
import com.caracolas.gestortrainer.feature.scoring.api.ScoreRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Inicio del alumno: carrusel de categorías populares, mi puesto/puntos/racha y top 3 de trainers. */
@Composable
fun ClientHomeV2Screen(
    clientId: ClientId,
    discoveryService: DiscoveryService,
    leaderboardService: LeaderboardService,
    scoreRepository: ScoreRepository,
    assignmentService: AssignmentService,
    messagingService: MessagingService,
    onOpenCategory: (String) -> Unit,
    onOpenLeaderboards: () -> Unit,
    onOpenMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userId = remember(clientId) { UserId(clientId.value) }
    val categories by discoveryService.popularCategories().collectAsState(initial = emptyList())
    val points by scoreRepository.observeTotal(userId).collectAsState(initial = null)
    val unread by messagingService.observeConversations(userId).collectAsState(initial = emptyList())
    val topTrainers by leaderboardService.observe(BoardType.GLOBAL_TRAINERS, BoardScope.Global, PageRequest(size = 3))
        .collectAsState(initial = null)
    var position by remember { mutableStateOf<Int?>(null) }
    var streak by remember { mutableIntStateOf(0) }

    LaunchedEffect(userId, points) {
        position = leaderboardService.myPosition(BoardType.GLOBAL_USERS, userId).getOrNull()?.position
        streak = weeklyStreak(assignmentService, clientId, LocalDate.now())
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Inicio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val unreadTotal = unread.sumOf { it.unread }
            SecondaryButton(
                text = if (unreadTotal > 0) "Mensajes · $unreadTotal" else "Mensajes",
                onClick = onOpenMessages,
            )
        }

        if (categories.isNotEmpty()) {
            CategoryCarousel(categories, onOpenCategory)
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Mi puesto", style = MaterialTheme.typography.labelMedium); Text(position?.let { "#$it" } ?: "—", fontWeight = FontWeight.Bold) }
                Column { Text("Puntos", style = MaterialTheme.typography.labelMedium); Text(points?.value?.toString() ?: "0", fontWeight = FontWeight.Bold) }
                Column { Text("Racha", style = MaterialTheme.typography.labelMedium); Text("$streak sem.", fontWeight = FontWeight.Bold) }
            }
        }

        Text("Top trainers", fontWeight = FontWeight.Bold)
        topTrainers?.entries?.forEach { entry ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${entry.position}. ${entry.displayName}")
                Text(entry.score.toInt().toString(), fontWeight = FontWeight.Bold)
            }
        }
        PrimaryButton(text = "Ver clasificaciones", onClick = onOpenLeaderboards, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CategoryCarousel(categories: List<PopularCategory>, onOpenCategory: (String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()
    var autoAdvance by remember { mutableStateOf(true) }
    LaunchedEffect(isDragged) { if (isDragged) autoAdvance = false }
    LaunchedEffect(categories.size) {
        while (autoAdvance && categories.size > 1) {
            delay(5000)
            if (autoAdvance) {
                val next = (pagerState.currentPage + 1) % categories.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "‹",
                modifier = Modifier.clickable {
                    autoAdvance = false
                    val prev = (pagerState.currentPage - 1 + categories.size) % categories.size
                    scope.launch { pagerState.animateScrollToPage(prev) }
                }.padding(8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).height(140.dp)) { page ->
                CategoryCard(categories[page], onOpenCategory)
            }
            Text(
                "›",
                modifier = Modifier.clickable {
                    autoAdvance = false
                    val next = (pagerState.currentPage + 1) % categories.size
                    scope.launch { pagerState.animateScrollToPage(next) }
                }.padding(8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            categories.indices.forEach { index ->
                Box(
                    modifier = Modifier.padding(3.dp).size(7.dp)
                        .background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: PopularCategory, onOpenCategory: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .clickable { onOpenCategory(category.discipline) },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(category.discipline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Column {
            Text("${category.trainerCount} trainers")
            Text("Ver trainers", fontWeight = FontWeight.Bold)
        }
    }
}

/** Semanas completas seguidas hasta [date] (inclusive). Misma idea que ScoringCoordinator, aquí solo para mostrarla. */
private suspend fun weeklyStreak(assignmentService: AssignmentService, clientId: ClientId, date: LocalDate): Int {
    var streak = 0
    var weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    repeat(104) {
        val week = assignmentService.observeCalendar(clientId, DateRange(weekStart, weekStart.plusDays(6))).first()
        val completed = week.isNotEmpty() && week.all { it.status == AssignmentStatus.DONE }
        if (!completed) return streak
        streak++
        weekStart = weekStart.minusWeeks(1)
    }
    return streak
}
