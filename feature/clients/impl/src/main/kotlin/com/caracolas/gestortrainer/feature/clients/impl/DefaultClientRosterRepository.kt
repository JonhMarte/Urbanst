package com.caracolas.gestortrainer.feature.clients.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.feature.clients.api.ClientCard
import com.caracolas.gestortrainer.feature.clients.api.ClientRosterRepository
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** El resumen de hoy y los puntos llegan con tracking y scoring (Fase 2); aquí van con valores por defecto. */
class DefaultClientRosterRepository @Inject constructor(
    private val linkService: DefaultLinkService,
    private val profileRepository: ProfileRepository,
) : ClientRosterRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveClients(trainerId: TrainerId): Flow<List<ClientCard>> =
        linkService.acceptedLinksFor(trainerId).flatMapLatest { clientIds ->
            if (clientIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(clientIds.map { profileRepository.observeClient(it) }) { profiles ->
                    profiles.filterNotNull().map { it.toCard() }
                }
            }
        }

    private fun ClientProfile.toCard() = ClientCard(
        clientId = ClientId(userId.value),
        displayName = personal.fullName,
        photoUrl = photoUrl,
        todaySummary = "Sin actividades hoy",
        points = 0,
    )
}
