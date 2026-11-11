package com.caracolas.gestortrainer.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncOperationConverterTest {

    private val converter = SyncOperationConverter()

    @Test
    fun `convierte cada operacion a su nombre y de vuelta sin perder informacion`() {
        SyncOperation.entries.forEach { operation ->
            val stored = converter.fromOperation(operation)
            assertEquals(operation, converter.toOperation(stored))
        }
    }
}
