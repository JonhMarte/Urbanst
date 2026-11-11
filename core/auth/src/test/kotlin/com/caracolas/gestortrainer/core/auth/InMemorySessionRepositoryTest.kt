package com.caracolas.gestortrainer.core.auth

import com.caracolas.gestortrainer.core.model.PublicId
import com.caracolas.gestortrainer.core.model.Role
import com.caracolas.gestortrainer.core.model.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemorySessionRepositoryTest {

    @Test
    fun `al empezar no hay sesion activa`() = runTest {
        assertNull(InMemorySessionRepository().session.value)
    }

    @Test
    fun `startSession abre la sesion indicada`() = runTest {
        val repository = InMemorySessionRepository()
        val session = Session(UserId("u1"), PublicId("TR-00001"), Role.TRAINER)

        val result = repository.startSession(session)

        assertTrue(result.isSuccess)
        assertTrue(repository.session.value == session)
    }

    @Test
    fun `cerrar sesion la deja vacia`() = runTest {
        val repository = InMemorySessionRepository()
        repository.startSession(Session(UserId("u1"), PublicId("TR-00001"), Role.TRAINER))

        repository.signOut()

        assertNull(repository.session.value)
    }

    @Test
    fun `signIn con credenciales todavia no esta soportado`() = runTest {
        val result = InMemorySessionRepository().signIn("a@b.com", "x")
        assertTrue(result.isFailure)
    }
}
