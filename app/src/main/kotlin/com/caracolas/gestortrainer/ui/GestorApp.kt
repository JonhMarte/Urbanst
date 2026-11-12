package com.caracolas.gestortrainer.ui

import androidx.compose.foundation.clickable
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
import com.caracolas.gestortrainer.core.model.ConversationId
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.activities.api.ActivityValidator
import com.caracolas.gestortrainer.feature.activities.impl.ActivityLibraryScreen
import com.caracolas.gestortrainer.feature.ads.api.AdPlacement
import com.caracolas.gestortrainer.feature.ads.api.AdProvider
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentRepository
import com.caracolas.gestortrainer.feature.assessment.api.AssessmentValidator
import com.caracolas.gestortrainer.feature.assessment.impl.AssessmentQuestionnaireScreen
import com.caracolas.gestortrainer.feature.assessment.impl.TrainerClientProfileScreen
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.calendar.impl.CalendarScreen
import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.clients.api.TrainerNotesService
import com.caracolas.gestortrainer.feature.clients.impl.ClientsScreen
import com.caracolas.gestortrainer.feature.discovery.api.DiscoveryService
import com.caracolas.gestortrainer.feature.discovery.api.TrainerSummary
import com.caracolas.gestortrainer.feature.discovery.impl.ClientHomeV2Screen
import com.caracolas.gestortrainer.feature.discovery.impl.DiscoveryScreen
import com.caracolas.gestortrainer.feature.groups.stub.GroupsEntryButton
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardService
import com.caracolas.gestortrainer.feature.leaderboards.impl.LeaderboardScreen
import com.caracolas.gestortrainer.feature.matching.api.MatchingService
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequest
import com.caracolas.gestortrainer.feature.matching.api.TrainerRequestStatus
import com.caracolas.gestortrainer.feature.matching.impl.ClientProposalScreen
import com.caracolas.gestortrainer.feature.matching.impl.ClientProposalsListScreen
import com.caracolas.gestortrainer.feature.matching.impl.ClientRequestScreen
import com.caracolas.gestortrainer.feature.matching.impl.ProposalComposerScreen
import com.caracolas.gestortrainer.feature.matching.impl.TrainerPublicProfileScreen
import com.caracolas.gestortrainer.feature.matching.impl.TrainerRequestsScreen
import com.caracolas.gestortrainer.feature.media.api.MediaUploader
import com.caracolas.gestortrainer.feature.media.api.MediaViewerRegistry
import com.caracolas.gestortrainer.feature.messaging.api.MessagingService
import com.caracolas.gestortrainer.feature.messaging.impl.ChatScreen
import com.caracolas.gestortrainer.feature.muscles.api.MuscleCatalog
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import com.caracolas.gestortrainer.feature.nutrition.stub.NutritionEntryButton
import com.caracolas.gestortrainer.feature.onboarding.api.OnboardingService
import com.caracolas.gestortrainer.feature.onboarding.impl.OnboardingScreen
import com.caracolas.gestortrainer.feature.payments.api.GatewayId
import com.caracolas.gestortrainer.feature.payments.api.PaymentRequestService
import com.caracolas.gestortrainer.feature.payments.api.PayoutProviderRegistry
import com.caracolas.gestortrainer.feature.payments.impl.ClientPaymentScreen
import com.caracolas.gestortrainer.feature.payments.impl.PayoutLinkScreen
import com.caracolas.gestortrainer.feature.payments.impl.PayoutLinkStatusScreen
import com.caracolas.gestortrainer.feature.payments.impl.TrainerPaymentsScreen
import com.caracolas.gestortrainer.feature.plan.api.WeeklyPlanService
import com.caracolas.gestortrainer.feature.plan.impl.WeeklyPlanScreen
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import com.caracolas.gestortrainer.feature.profile.impl.ProfileScreen
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import com.caracolas.gestortrainer.feature.reviews.impl.ReviewsScreen
import com.caracolas.gestortrainer.feature.scoring.api.ScoreRepository
import com.caracolas.gestortrainer.feature.tracking.api.ActivityTracker
import kotlinx.coroutines.flow.first
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
    val scoreRepository: ScoreRepository,
    val assessmentRepository: AssessmentRepository,
    val assessmentValidator: AssessmentValidator,
    val discoveryService: DiscoveryService,
    val messagingService: MessagingService,
    val paymentRequestService: PaymentRequestService,
    val payoutProviderRegistry: PayoutProviderRegistry,
    val matchingService: MatchingService,
)

private enum class Tab(val label: String) {
    HOME("Inicio"), USERS("Usuarios"), ACTIVITIES("Actividades"),
    DISCOVER("Actividad"), PLAN("Mi plan"), PROFILE("Perfil"),
}

private fun tabsFor(role: Role) = when (role) {
    Role.TRAINER -> listOf(Tab.HOME, Tab.USERS, Tab.ACTIVITIES, Tab.PROFILE)
    Role.CLIENT -> listOf(Tab.HOME, Tab.DISCOVER, Tab.PLAN, Tab.PROFILE)
}

/** A quién se le está escribiendo; se resuelve una vez y se reutiliza mientras dure la pantalla de chat. */
private data class ChatTarget(val conversationId: ConversationId, val otherName: String)

/**
 * Pantallas del pipeline "solicitud -> propuesta -> pago" del descubrimiento (feature:matching).
 * Va en paralelo al vínculo por ID (feature:clients), que no cambia.
 */
private sealed interface MatchingOverlay {
    data class TrainerProfile(val summary: TrainerSummary) : MatchingOverlay
    data class ClientRequestForm(val summary: TrainerSummary) : MatchingOverlay
    data object TrainerInbox : MatchingOverlay
    data class ProposalComposer(val request: TrainerRequest) : MatchingOverlay
    data class ClientProposalsList(val fromProfile: Boolean) : MatchingOverlay
    data class ClientProposalDetail(val request: TrainerRequest) : MatchingOverlay
    data class FullQuestionnaire(val clientId: ClientId) : MatchingOverlay
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
    // Tras registrarse como alumno, se ofrece el cuestionario antes de entrar (se puede saltar).
    var pendingQuestionnaireForClientId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(activeSession) {
        if (activeSession == null) {
            chosenRole = null
        } else if (recentSessions.none { it.userId == activeSession.userId }) {
            recentSessions = recentSessions + activeSession
        }
    }

    val current = activeSession
    if (current != null) {
        if (current.role == Role.CLIENT && pendingQuestionnaireForClientId == current.userId.value) {
            AssessmentQuestionnaireScreen(
                clientId = ClientId(current.userId.value),
                accountEmail = "",
                assessmentRepository = deps.assessmentRepository,
                assessmentValidator = deps.assessmentValidator,
                muscleMapRenderer = deps.muscleMapRenderer,
                onDone = { pendingQuestionnaireForClientId = null },
                onSkip = { pendingQuestionnaireForClientId = null },
            )
            return
        }
        RoleShell(session = current, deps = deps)
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
        OnboardingScreen(
            role = role,
            onboardingService = deps.onboardingService,
            onRegistered = { userId -> if (role == Role.CLIENT) pendingQuestionnaireForClientId = userId.value },
        )
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
    var showCalendarForSelected by remember { mutableStateOf(false) }
    var showLeaderboards by remember { mutableStateOf(false) }
    var discoveryInitialDiscipline by remember { mutableStateOf<String?>(null) }
    var clientProfileSection by remember { mutableStateOf(ClientProfileSection.HOME) }
    var trainerPaymentsSection by remember { mutableStateOf(TrainerPaymentsSection.HIDDEN) }
    var linkedPayoutProvider by remember { mutableStateOf<GatewayId?>(null) }
    var activeChat by remember { mutableStateOf<ChatTarget?>(null) }
    var matchingOverlay by remember { mutableStateOf<MatchingOverlay?>(null) }
    val scope = rememberCoroutineScope()

    val chat = activeChat
    if (chat != null) {
        ChatScreen(
            conversationId = chat.conversationId,
            currentUserId = UserId(session.userId.value),
            otherDisplayName = chat.otherName,
            messagingService = deps.messagingService,
            onBack = { activeChat = null },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    fun openChatWithTrainer(trainerId: TrainerId, clientId: ClientId, otherName: String) {
        scope.launch {
            val conversationId = deps.messagingService.conversationWith(trainerId, clientId).getOrNull() ?: return@launch
            activeChat = ChatTarget(conversationId, otherName)
        }
    }

    val overlay = matchingOverlay
    if (overlay != null) {
        MatchingOverlayHost(
            overlay = overlay,
            session = session,
            deps = deps,
            onChangeOverlay = { matchingOverlay = it },
            onOpenChat = { trainerId, clientId, name -> openChatWithTrainer(trainerId, clientId, name) },
        )
        return
    }

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
            Tab.HOME -> if (role == Role.TRAINER) {
                TrainerHome(
                    session, deps, modifier,
                    onOpenPayments = { selected = Tab.PROFILE; trainerPaymentsSection = TrainerPaymentsSection.HOME },
                )
            } else {
                val clientId = ClientId(session.userId.value)
                if (showLeaderboards) {
                    LeaderboardScreen(
                        viewerUserId = UserId(session.userId.value),
                        leaderboardService = deps.leaderboardService,
                        modifier = modifier,
                    )
                } else {
                    ClientHomeV2Screen(
                        clientId = clientId,
                        discoveryService = deps.discoveryService,
                        leaderboardService = deps.leaderboardService,
                        scoreRepository = deps.scoreRepository,
                        assignmentService = deps.assignmentService,
                        messagingService = deps.messagingService,
                        onOpenCategory = { discipline -> discoveryInitialDiscipline = discipline; selected = Tab.DISCOVER },
                        onOpenLeaderboards = { showLeaderboards = true },
                        onOpenMessages = {
                            scope.launch {
                                val trainerId = deps.linkService.observeLinkedTrainers(clientId).first().firstOrNull()
                                if (trainerId != null) {
                                    val name = deps.profileRepository.observeTrainer(trainerId).first()?.personal?.fullName ?: trainerId.value
                                    openChatWithTrainer(trainerId, clientId, name)
                                }
                            }
                        },
                        modifier = modifier,
                    )
                }
            }
            Tab.USERS -> {
                val clientIdValue = selectedClientId
                when {
                    clientIdValue != null && showCalendarForSelected -> CalendarScreen(
                        trainerId = TrainerId(session.userId.value),
                        clientId = ClientId(clientIdValue),
                        assignmentService = deps.assignmentService,
                        activityRepository = deps.activityRepository,
                        profileRepository = deps.profileRepository,
                        trainerNotesService = deps.trainerNotesService,
                        onBack = { showCalendarForSelected = false },
                        modifier = modifier,
                    )
                    clientIdValue != null -> TrainerClientProfileScreen(
                        clientId = ClientId(clientIdValue),
                        assessmentRepository = deps.assessmentRepository,
                        profileRepository = deps.profileRepository,
                        muscleMapRenderer = deps.muscleMapRenderer,
                        onOpenMessage = {
                            scope.launch {
                                val name = deps.profileRepository.observeClient(ClientId(clientIdValue)).first()?.personal?.fullName ?: clientIdValue
                                openChatWithTrainer(TrainerId(session.userId.value), ClientId(clientIdValue), name)
                            }
                        },
                        onOpenCalendar = { showCalendarForSelected = true },
                        onBack = { selectedClientId = null },
                        modifier = modifier,
                    )
                    else -> ClientsScreen(
                        trainerId = TrainerId(session.userId.value),
                        linkService = deps.linkService,
                        clientRosterRepository = deps.clientRosterRepository,
                        profileRepository = deps.profileRepository,
                        modifier = modifier,
                        onSelectClient = { selectedClientId = it.value; showCalendarForSelected = false },
                        extraActions = {
                            SecondaryButton(
                                "Solicitudes de alumnos",
                                onClick = { matchingOverlay = MatchingOverlay.TrainerInbox },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
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
                discoveryService = deps.discoveryService,
                initialDiscipline = discoveryInitialDiscipline,
                modifier = modifier,
                onOpenProfile = { summary -> matchingOverlay = MatchingOverlay.TrainerProfile(summary) },
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
            Tab.PROFILE -> if (role == Role.TRAINER) {
                TrainerProfileWithPayments(session, deps, modifier, trainerPaymentsSection, { trainerPaymentsSection = it }, linkedPayoutProvider, { linkedPayoutProvider = it })
            } else {
                ClientProfileHub(
                    session = session,
                    deps = deps,
                    modifier = modifier,
                    section = clientProfileSection,
                    onSectionChange = { clientProfileSection = it },
                    onOpenChat = { trainerId, name -> openChatWithTrainer(trainerId, ClientId(session.userId.value), name) },
                    onOpenProposals = { matchingOverlay = MatchingOverlay.ClientProposalsList(fromProfile = true) },
                )
            }
        }
    }
}

private enum class ClientProfileSection { HOME, PERSONAL_DATA, ASSESSMENT, PAYMENT }
private enum class TrainerPaymentsSection { HIDDEN, HOME, LINK, STATUS }

@Composable
private fun TrainerHome(
    session: Session,
    deps: AppDependencies,
    modifier: Modifier,
    onOpenPayments: () -> Unit,
) {
    val trainerId = TrainerId(session.userId.value)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Inicio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
private fun TrainerProfileWithPayments(
    session: Session,
    deps: AppDependencies,
    modifier: Modifier,
    section: TrainerPaymentsSection,
    onSectionChange: (TrainerPaymentsSection) -> Unit,
    linkedProvider: GatewayId?,
    onLinkedProviderChange: (GatewayId?) -> Unit,
) {
    val trainerId = TrainerId(session.userId.value)
    when (section) {
        TrainerPaymentsSection.HIDDEN -> Column(modifier = modifier.fillMaxSize()) {
            ProfileScreen(session, deps.profileRepository, deps.profileValidator, deps.sessionRepository, Modifier.weight(1f))
            SecondaryButton(
                text = "Habilitación de pagos",
                onClick = { onSectionChange(TrainerPaymentsSection.HOME) },
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            )
        }
        TrainerPaymentsSection.HOME -> TrainerPaymentsScreen(
            trainerId = trainerId,
            paymentRequestService = deps.paymentRequestService,
            payoutProviderRegistry = deps.payoutProviderRegistry,
            linkService = deps.linkService,
            profileRepository = deps.profileRepository,
            onOpenPayoutLink = { onSectionChange(TrainerPaymentsSection.LINK) },
            onBack = { onSectionChange(TrainerPaymentsSection.HIDDEN) },
            modifier = modifier,
        )
        TrainerPaymentsSection.LINK -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            PayoutLinkScreen(
                trainerId = trainerId,
                payoutProviderRegistry = deps.payoutProviderRegistry,
                onContinue = { url, provider ->
                    try {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    } catch (_: android.content.ActivityNotFoundException) {
                        // Sin navegador disponible: seguimos igualmente al estado de verificación.
                    }
                    onLinkedProviderChange(provider)
                    onSectionChange(TrainerPaymentsSection.STATUS)
                },
                onBack = { onSectionChange(TrainerPaymentsSection.HOME) },
                modifier = modifier,
            )
        }
        TrainerPaymentsSection.STATUS -> linkedProvider?.let { provider ->
            PayoutLinkStatusScreen(
                trainerId = trainerId,
                provider = provider,
                payoutProviderRegistry = deps.payoutProviderRegistry,
                onBackToPayments = { onSectionChange(TrainerPaymentsSection.HOME) },
                onChooseAnother = { onSectionChange(TrainerPaymentsSection.LINK) },
                onReopenProviderPage = {},
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ClientProfileHub(
    session: Session,
    deps: AppDependencies,
    modifier: Modifier,
    section: ClientProfileSection,
    onSectionChange: (ClientProfileSection) -> Unit,
    onOpenChat: (TrainerId, String) -> Unit,
    onOpenProposals: () -> Unit,
) {
    val clientId = ClientId(session.userId.value)
    when (section) {
        ClientProfileSection.HOME -> {
            val pending by deps.paymentRequestService.observePendingForClient(clientId).collectAsState(initial = emptyList())
            val linkedTrainers by deps.linkService.observeLinkedTrainers(clientId).collectAsState(initial = emptyList())
            val proposals by deps.matchingService.observeForClient(clientId).collectAsState(initial = emptyList())
            val pendingProposals = proposals.count { it.status == TrainerRequestStatus.PROPOSAL_SENT }
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (pending.isNotEmpty()) {
                    HubRow("Pago pendiente · ${pending.size}") { onSectionChange(ClientProfileSection.PAYMENT) }
                }
                if (pendingProposals > 0) {
                    HubRow("Propuestas · $pendingProposals") { onOpenProposals() }
                }
                HubRow("Mis objetivos y hábitos") { onSectionChange(ClientProfileSection.ASSESSMENT) }
                HubRow("Datos personales") { onSectionChange(ClientProfileSection.PERSONAL_DATA) }
                val coroutineScope = rememberCoroutineScope()
                HubRow("Mensajes con mi trainer") {
                    val trainerId = linkedTrainers.firstOrNull() ?: return@HubRow
                    coroutineScope.launch {
                        val name = deps.profileRepository.observeTrainer(trainerId).first()?.personal?.fullName ?: trainerId.value
                        onOpenChat(trainerId, name)
                    }
                }
            }
        }
        ClientProfileSection.PERSONAL_DATA -> Column(modifier = modifier.fillMaxSize()) {
            SecondaryButton(text = "← Volver", onClick = { onSectionChange(ClientProfileSection.HOME) }, modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 0.dp))
            ProfileScreen(session, deps.profileRepository, deps.profileValidator, deps.sessionRepository, Modifier.weight(1f))
        }
        ClientProfileSection.ASSESSMENT -> AssessmentQuestionnaireScreen(
            clientId = clientId,
            accountEmail = "",
            assessmentRepository = deps.assessmentRepository,
            assessmentValidator = deps.assessmentValidator,
            muscleMapRenderer = deps.muscleMapRenderer,
            onDone = { onSectionChange(ClientProfileSection.HOME) },
            onSkip = null,
            modifier = modifier,
        )
        ClientProfileSection.PAYMENT -> ClientPaymentScreen(
            clientId = clientId,
            paymentRequestService = deps.paymentRequestService,
            profileRepository = deps.profileRepository,
            onBack = { onSectionChange(ClientProfileSection.HOME) },
            modifier = modifier,
        )
    }
}

/** Un único host para las 5 pantallas del pipeline de descubrimiento; una pantalla a la vez, a pantalla completa. */
@Composable
private fun MatchingOverlayHost(
    overlay: MatchingOverlay,
    session: Session,
    deps: AppDependencies,
    onChangeOverlay: (MatchingOverlay?) -> Unit,
    onOpenChat: (TrainerId, ClientId, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    when (overlay) {
        is MatchingOverlay.TrainerProfile -> TrainerPublicProfileScreen(
            trainerId = overlay.summary.trainerId,
            profileRepository = deps.profileRepository,
            reviewService = deps.reviewService,
            onSendRequest = { onChangeOverlay(MatchingOverlay.ClientRequestForm(overlay.summary)) },
            onBack = { onChangeOverlay(null) },
        )
        is MatchingOverlay.ClientRequestForm -> ClientRequestScreen(
            clientId = ClientId(session.userId.value),
            trainerPublicId = overlay.summary.publicId,
            trainerDisplayName = overlay.summary.displayName,
            matchingService = deps.matchingService,
            onSent = { onChangeOverlay(null) },
            onBack = { onChangeOverlay(MatchingOverlay.TrainerProfile(overlay.summary)) },
        )
        is MatchingOverlay.TrainerInbox -> TrainerRequestsScreen(
            trainerId = TrainerId(session.userId.value),
            matchingService = deps.matchingService,
            profileRepository = deps.profileRepository,
            onOpenProposalComposer = { request -> onChangeOverlay(MatchingOverlay.ProposalComposer(request)) },
            onOpenFullQuestionnaire = { request -> onChangeOverlay(MatchingOverlay.FullQuestionnaire(request.clientId)) },
            onBack = { onChangeOverlay(null) },
        )
        is MatchingOverlay.ProposalComposer -> ProposalComposerScreen(
            request = overlay.request,
            matchingService = deps.matchingService,
            onSent = { onChangeOverlay(MatchingOverlay.TrainerInbox) },
            onBack = { onChangeOverlay(MatchingOverlay.TrainerInbox) },
        )
        is MatchingOverlay.ClientProposalsList -> ClientProposalsListScreen(
            clientId = ClientId(session.userId.value),
            matchingService = deps.matchingService,
            profileRepository = deps.profileRepository,
            onOpenProposal = { request -> onChangeOverlay(MatchingOverlay.ClientProposalDetail(request)) },
            onBack = { onChangeOverlay(null) },
        )
        is MatchingOverlay.ClientProposalDetail -> ClientProposalScreen(
            request = overlay.request,
            matchingService = deps.matchingService,
            profileRepository = deps.profileRepository,
            onAccepted = { onChangeOverlay(null) },
            onRejected = {
                scope.launch { deps.matchingService.reject(overlay.request.id) }
                onChangeOverlay(MatchingOverlay.ClientProposalsList(fromProfile = true))
            },
            onBack = { onChangeOverlay(MatchingOverlay.ClientProposalsList(fromProfile = true)) },
        )
        is MatchingOverlay.FullQuestionnaire -> TrainerClientProfileScreen(
            clientId = overlay.clientId,
            assessmentRepository = deps.assessmentRepository,
            profileRepository = deps.profileRepository,
            muscleMapRenderer = deps.muscleMapRenderer,
            onOpenMessage = {
                scope.launch {
                    val name = deps.profileRepository.observeClient(overlay.clientId).first()?.personal?.fullName ?: overlay.clientId.value
                    onOpenChat(TrainerId(session.userId.value), overlay.clientId, name)
                }
            },
            onOpenCalendar = {},
            onBack = { onChangeOverlay(MatchingOverlay.TrainerInbox) },
        )
    }
}

@Composable
private fun HubRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}
