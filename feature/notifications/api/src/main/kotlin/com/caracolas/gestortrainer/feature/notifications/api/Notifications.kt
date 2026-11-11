package com.caracolas.gestortrainer.feature.notifications.api

import com.caracolas.gestortrainer.core.model.UserId

/** Avisos push (Firebase Cloud Messaging). */
interface NotificationService {
    suspend fun registerDevice(userId: UserId, token: String): Result<Unit>
    suspend fun unregisterDevice(userId: UserId): Result<Unit>
}
