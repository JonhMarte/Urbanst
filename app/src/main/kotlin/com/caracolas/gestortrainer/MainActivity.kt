package com.caracolas.gestortrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.caracolas.gestortrainer.core.auth.SessionRepository
import com.caracolas.gestortrainer.core.designsystem.GestorTheme
import com.caracolas.gestortrainer.core.featureflags.FeatureFlags
import com.caracolas.gestortrainer.feature.activities.api.ActivityRepository
import com.caracolas.gestortrainer.feature.activities.api.ActivityValidator
import com.caracolas.gestortrainer.feature.ads.api.AdProvider
import com.caracolas.gestortrainer.feature.calendar.api.AssignmentService
import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.feature.clients.api.LinkService
import com.caracolas.gestortrainer.feature.clients.api.TrainerNotesService
import com.caracolas.gestortrainer.feature.media.api.MediaUploader
import com.caracolas.gestortrainer.feature.media.api.MediaViewerRegistry
import com.caracolas.gestortrainer.feature.muscles.api.MuscleCatalog
import com.caracolas.gestortrainer.feature.muscles.api.MuscleMapRenderer
import com.caracolas.gestortrainer.feature.onboarding.api.OnboardingService
import com.caracolas.gestortrainer.feature.leaderboards.api.LeaderboardService
import com.caracolas.gestortrainer.feature.plan.api.WeeklyPlanService
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import com.caracolas.gestortrainer.feature.profile.api.ProfileValidator
import com.caracolas.gestortrainer.feature.reviews.api.ReviewService
import com.caracolas.gestortrainer.feature.tracking.api.ActivityTracker
import com.caracolas.gestortrainer.ui.AppDependencies
import com.caracolas.gestortrainer.ui.GestorApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var featureFlags: FeatureFlags
    @Inject lateinit var muscleMapRenderer: MuscleMapRenderer
    @Inject lateinit var adProvider: AdProvider
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var onboardingService: OnboardingService
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var profileValidator: ProfileValidator
    @Inject lateinit var linkService: LinkService
    @Inject lateinit var clientRosterRepository: ClientRosterRepository
    @Inject lateinit var muscleCatalog: MuscleCatalog
    @Inject lateinit var activityRepository: ActivityRepository
    @Inject lateinit var activityValidator: ActivityValidator
    @Inject lateinit var mediaUploader: MediaUploader
    @Inject lateinit var mediaViewerRegistry: MediaViewerRegistry
    @Inject lateinit var assignmentService: AssignmentService
    @Inject lateinit var weeklyPlanService: WeeklyPlanService
    @Inject lateinit var trainerNotesService: TrainerNotesService
    @Inject lateinit var activityTracker: ActivityTracker
    @Inject lateinit var leaderboardService: LeaderboardService
    @Inject lateinit var reviewService: ReviewService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deps = AppDependencies(
            featureFlags = featureFlags,
            muscleMapRenderer = muscleMapRenderer,
            adProvider = adProvider,
            sessionRepository = sessionRepository,
            onboardingService = onboardingService,
            profileRepository = profileRepository,
            profileValidator = profileValidator,
            linkService = linkService,
            clientRosterRepository = clientRosterRepository,
            muscleCatalog = muscleCatalog,
            activityRepository = activityRepository,
            activityValidator = activityValidator,
            mediaUploader = mediaUploader,
            mediaViewerRegistry = mediaViewerRegistry,
            assignmentService = assignmentService,
            weeklyPlanService = weeklyPlanService,
            trainerNotesService = trainerNotesService,
            activityTracker = activityTracker,
            leaderboardService = leaderboardService,
            reviewService = reviewService,
        )
        setContent {
            GestorTheme {
                GestorApp(deps)
            }
        }
    }
}
