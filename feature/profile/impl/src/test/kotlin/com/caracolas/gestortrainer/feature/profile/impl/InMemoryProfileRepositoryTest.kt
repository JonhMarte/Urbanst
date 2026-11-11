package com.caracolas.gestortrainer.feature.profile.impl

import com.caracolas.gestortrainer.core.model.ClientId
import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Region
import com.caracolas.gestortrainer.core.model.TrainerId
import com.caracolas.gestortrainer.core.model.UserId
import com.caracolas.gestortrainer.feature.profile.api.ClientProfile
import com.caracolas.gestortrainer.feature.profile.api.PersonalData
import com.caracolas.gestortrainer.feature.profile.api.TrainerProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryProfileRepositoryTest {
    private val repository = InMemoryProfileRepository()

    @Test
    fun guardarYObservarUnTrainerDevuelveElMismoPerfil() = runBlocking {
        val userId = UserId("u1")
        val profile = TrainerProfile(
            userId = userId,
            publicId = PublicId("TR-00001"),
            personal = PersonalData("Carlos Méndez", "Española", Region("ES"), age = 34),
            mainDiscipline = "Jiu-jitsu",
            secondaryDisciplines = emptyList(),
            yearsAsTrainer = 5,
            bio = "",
            photoUrl = null,
        )

        repository.saveTrainer(profile)

        assertEquals(profile, repository.observeTrainer(TrainerId(userId.value)).first())
    }

    @Test
    fun unClientSinGuardarDevuelveNull() = runBlocking {
        assertNull(repository.observeClient(ClientId("desconocido")).first())
    }

    @Test
    fun guardarUnClientNoAfectaAOtroCliente() = runBlocking {
        val profileA = ClientProfile(
            userId = UserId("a"),
            publicId = PublicId("AL-00001"),
            personal = PersonalData("Alumna A", "Española", Region("ES"), age = 29),
            body = null,
            photoUrl = null,
        )

        repository.saveClient(profileA)

        assertNull(repository.observeClient(ClientId("b")).first())
        assertEquals(profileA, repository.observeClient(ClientId("a")).first())
    }
}
