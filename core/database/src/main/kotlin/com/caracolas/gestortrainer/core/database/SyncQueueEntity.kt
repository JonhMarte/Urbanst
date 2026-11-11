package com.caracolas.gestortrainer.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncOperation { CREATE, UPDATE, DELETE }

/** Cola de cambios pendientes de enviar al backend cuando no hay red. */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperation,
    val payloadJson: String,
    val createdAtEpochMillis: Long,
)
