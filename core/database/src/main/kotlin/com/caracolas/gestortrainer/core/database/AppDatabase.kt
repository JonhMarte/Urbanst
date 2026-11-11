package com.caracolas.gestortrainer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SyncQueueEntity::class], version = 1, exportSchema = true)
@TypeConverters(SyncOperationConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao
}
