package com.caracolas.gestortrainer.core.database

import android.content.Context
import androidx.room.Room

/** Construye la base de datos Room única de la app. */
object DatabaseFactory {
    fun create(context: Context): AppDatabase =
        Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DatabaseConfig.NAME).build()
}
