package com.caracolas.gestortrainer.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMillis ASC")
    fun observeAll(): Flow<List<SyncQueueEntity>>

    @Delete
    suspend fun remove(item: SyncQueueEntity)
}
