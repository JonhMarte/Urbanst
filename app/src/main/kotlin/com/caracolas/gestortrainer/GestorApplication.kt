package com.caracolas.gestortrainer

import android.app.Application
import com.caracolas.gestortrainer.feature.messaging.impl.MessagingLinkCoordinator
import com.caracolas.gestortrainer.feature.scoring.impl.ScoringCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GestorApplication : Application() {
    @Inject lateinit var scoringCoordinator: ScoringCoordinator
    @Inject lateinit var messagingLinkCoordinator: MessagingLinkCoordinator

    override fun onCreate() {
        super.onCreate()
        scoringCoordinator.start()
        messagingLinkCoordinator.start()
    }
}
