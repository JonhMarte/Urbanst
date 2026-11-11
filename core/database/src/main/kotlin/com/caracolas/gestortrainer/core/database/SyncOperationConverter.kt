package com.caracolas.gestortrainer.core.database

import androidx.room.TypeConverter

class SyncOperationConverter {
    @TypeConverter
    fun fromOperation(value: SyncOperation): String = value.name

    @TypeConverter
    fun toOperation(value: String): SyncOperation = SyncOperation.valueOf(value)
}
