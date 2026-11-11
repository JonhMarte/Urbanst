package com.caracolas.gestortrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import com.caracolas.gestortrainer.core.featureflags.FeatureFlags
import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.activities.api.ActivityValidator
import com.caracolas.gestortrainer.feature.activities.impl.ActivityLibraryScreen
import com.caracolas.gestortrainer.feature.ads.api.AdPlacement
import com.caracolas.gestortrainer.feature.ads.api.AdProvider
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.impl.CalendarScreen
import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.clients.api.TrainerNotesService
import com.caracolas.gestortrainer.feature.clients.impl.ClientsScreen
import com.caracolas.gestortrainer.feature.discovery.impl.DiscoveryScreen
import com.caracolas.gestortrainer.feature.groups.stub.GroupsEntryButton
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardService
import com.caracolas.gestortrainer.feature.leaderboards.impl.LeaderboardScreen
import com.caracolas.gestortrainer.feature.media.api.MediaUploader
import com.caracolas.gestortrainer.feature.media.api.MediaViewerRegistry
import com.caracolas.gestortrainer.feature.muscles.api.MuscleCatalog
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import com.caracolas.gestortrainer.feature.nutrition.stub.NutritionEntryButton
import com.caracolas.gestortrainer.feature.onboarding.api.OnboardingService
import com.caracolas.gestortrainer.feature.onboarding.impl.OnboardingScreen
import com.caracolas.gestortrainer.feature.plan.api.WeeklyPlanService
import com.caracolas.gestortrainer.feature.plan.impl.WeeklyPlanScreen
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import com.caracolas.gestortrainer.feature.profile.impl.ProfileScreen
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import com.caracolas.gestortrainer.feature.reviews.impl.ReviewsScreen
import com.caracolas.gestortrainer.feature.tracking.api.ActivityTracker
import kotlinx.coroutines.launch

/** Lo que la UI necesita de Hilt. Solo :app conoce las implementaciones. */
data class AppDependencies(
    val featureFlags: FeatureFlags,
    val muscleMapRenderer: MuscleMapRenderer,
    val adProvider: AdProvider,
    val sessionRepository: SessionRepository,
    val onboardingService: OnboardingService,
    val profileRepository: ProfileRepository,
    val profileValidator: ProfileValidator,
    val linkService: LinkService,
    val clientRosterRepository: ClientRosterRepository,
    val muscleCatalog: MuscleCatalog,
    val activityRepository: ActivityRepository,
    val activityValidator: ActivityValidator,
    val mediaUploader: MediaUploader,
    val mediaViewerRegistry: MediaViewerRegistry,
    val assignmentService: AssignmentService,
    val weeklyPlanService: WeeklyPlanService,
    val trainerNotesService: TrainerNotesService,
    val activityTracker: ActivityTracker,
    val leaderboardService: LeaderboardService,
    val reviewService: ReviewService,
)

private enum class Tab(val label: String) {
    HOME("Inicio"), USERS("Usuarios"), ACTIVITIES("Actividades"),
    DISCOVER("Actividad"), PLAN("Mi plan"), PROFILE("Perfil"),
}

private fun tabsFor(role: Role) = when (role) {
    Role.TRAINER -> listOf(Tab.HOME, Tab.USERS, Tab.ACTIVITIES, Tab.PROFILE)
    Role.CLIENT -> listOf(Tab.HOME, Tab.DISCOVER, Tab.PLAN, Tab.PROFILE)
}

@Composable
fun GestorApp(deps: AppDependencies) {
    val session by deps.sessionRepository.session.collectAsState(initial = null)
    val activeSession = session
    val scope = rememberCoroutineScope()

    // Rol elegido antes de registrarse; la sesión real se abre al completar el registro.
    // Se olvida cada vez que no hay sesión activa (incluida tras cerrar sesión), para volver a preguntar.
    var chosenRole by rememberSaveable { mutableStateOf<Role?>(null) }
    // Sin backend/login real: recordamos (solo en memoria, dentro de este proceso) las cuentas ya
    // creadas para poder volver a entrar sin registrar una nueva cada vez. Útil sobre todo para
    // probar en un único emulador flujos que involucran a dos cuentas (trainer + alumno).
    var recentSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    LaunchedEffect(activeSession) {
        if (activeSession == null) {
            chosenRole = null
        } else if (recentSessions.none { it.userId == activeSession.userId }) {
            recentSessions = recentSessions + activeSession
        }
    }

    if (activeSession != null) {
        RoleShell(session = activeSession, deps = deps)
        return
    }

    val role = chosenRole
    if (role == null) {
        RoleSelection(
            onSelect = { chosenRole = it },
            recentSessions = recentSessions,
            onReenter = { s -> scope.launch { deps.sessionRepository.startSession(s) } },
        )
    } else {
        OnboardingScreen(role = role, onboardingService = deps.onboardingService)
    }
}

@Composable
private fun RoleSelection(
    onSelect: (Role) -> Unit,
    recentSessions: List<Session> = emptyList(),
    onReenter: (Session) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("GestorTrainer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("¿Cómo vas a usar la app?", style = MaterialTheme.typography.bodyLarge)
        PrimaryButton("Soy trainer", onClick = { onSelect(Role.TRAINER) }, modifier = Modifier.fillMaxWidth())
        SecondaryButton("Soy alumno", onClick = { onSelect(Role.CLIENT) }, modifier = Modifier.fillMaxWidth())
        if (recentSessions.isNotEmpty()) {
            Text("Volver a entrar", style = MaterialTheme.typography.bodyLarge)
            recentSessions.forEach { s ->
                SecondaryButton(
                    "${s.role} · ${s.publicId.value}",
                    onClick = { onReenter(s) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RoleShell(session: Session, deps: AppDependencies) {
    val role = session.role
    val tabs = tabsFor(role)
    var selected by rememberSaveable(role) { mutableStateOf(Tab.HOME) }
    var selectedClientId by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { selected = tab },
                        icon = { Text(tab.label.take(1), fontWeight = FontWeight.Bold) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selected) {
            Tab.HOME -> if (role == Role.TRAINER) TrainerHome(session, deps, modifier) else ClientHome(session, deps, modifier)
            Tab.USERS -> {
                val clientId = selectedClientId
                if (clientId != null) {
                    CalendarScreen(
                        trainerId = TrainerId(session.userId.value),
                        clientId = ClientId(clientId),
                        assignmentService = deps.assignmentService,
                        activityRepository = deps.activityRepository,
                        profileRepository = deps.profileRepository,
                        trainerNotesService = deps.trainerNotesService,
                        onBack = { selectedClientId = null },
                        modifier = modifier,
                    )
                } else {
                    ClientsScreen(
                        trainerId = TrainerId(session.userId.value),
                        linkService = deps.linkService,
                        clientRosterRepository = deps.clientRosterRepository,
                        profileRepository = deps.profileRepository,
                        modifier = modifier,
                        onSelectClient = { selectedClientId = it.value },
                    )
                }
            }
            Tab.ACTIVITIES -> ActivityLibraryScreen(
                trainerId = TrainerId(session.userId.value),
                muscleMapRenderer = deps.muscleMapRenderer,
                muscleCatalog = deps.muscleCatalog,
                activityRepository = deps.activityRepository,
                activityValidator = deps.activityValidator,
                mediaUploader = deps.mediaUploader,
                mediaViewerRegistry = deps.mediaViewerRegistry,
                modifier = modifier,
                extraActions = { NutritionEntryButton(deps.featureFlags, onOpen = {}, modifier = Modifier.fillMaxWidth()) },
            )
            Tab.DISCOVER -> DiscoveryScreen(
                clientId = ClientId(session.userId.value),
                linkService = deps.linkService,
                profileRepository = deps.profileRepository,
                modifier = modifier,
            ) {
                GroupsEntryButton(deps.featureFlags, onOpen = {}, modifier = Modifier.fillMaxWidth())
                NutritionEntryButton(deps.featureFlags, onOpen = {}, modifier = Modifier.fillMaxWidth())
            }
            Tab.PLAN -> WeeklyPlanScreen(
                clientId = ClientId(session.userId.value),
                weeklyPlanService = deps.weeklyPlanService,
                activityTracker = deps.activityTracker,
                activityRepository = deps.activityRepository,
                mediaUploader = deps.mediaUploader,
                trainerNotesService = deps.trainerNotesService,
                modifier = modifier,
            )
            Tab.PROFILE -> ProfileScreen(session, deps.profileRepository, deps.profileValidator, deps.sessionRepository, modifier)
        }
    }
}

@Composable
private fun TrainerHome(session: Session, deps: AppDependencies, modifier: Modifier) {
    val trainerId = TrainerId(session.userId.value)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Inicio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        PrimaryButton("+ ADD New Activity", onClick = {}, modifier = Modifier.fillMaxWidth())
        SecondaryButton("+ ADD New User", onClick = {}, modifier = Modifier.fillMaxWidth())
        GroupsEntryButton(deps.featureFlags, onOpen = {}, modifier = Modifier.fillMaxWidth())
        LeaderboardScreen(
            viewerUserId = UserId(session.userId.value),
            viewerTrainerId = trainerId,
            leaderboardService = deps.leaderboardService,
            modifier = Modifier.fillMaxWidth(),
        )
        ReviewsScreen(
            trainerId = trainerId,
            reviewService = deps.reviewService,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ClientHome(session: Session, deps: AppDependencies, modifier: Modifier) {
    val clientId = ClientId(session.userId.value)
    val linkedTrainers by deps.linkService.observeLinkedTrainers(clientId).collectAsState(initial = emptyList())
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LeaderboardScreen(
            viewerUserId = UserId(session.userId.value),
            leaderboardService = deps.leaderboardService,
            modifier = Modifier.fillMaxWidth(),
        )
        linkedTrainers.firstOrNull()?.let { trainerId ->
            ReviewsScreen(
                trainerId = trainerId,
                reviewService = deps.reviewService,
                clientId = clientId,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        deps.adProvider.Banner(AdPlacement.CLIENT_HOME_BOTTOM, Modifier.padding(20.dp))
    }
}
