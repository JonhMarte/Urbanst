package com.caracolas.gestortrainer.core.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject

interface EventBus {
    val events: Flow<DomainEvent>
    suspend fun publish(event: DomainEvent)
}

/** Escucha solo un tipo de evento: bus.on<ActivitySessionCompleted>() */
inline fun <reified T : DomainEvent> EventBus.on(): Flow<T> = events.filterIsInstance()

/** Bus en memoria dentro de la app. En el backend el equivalente es EventBridge. */
class InMemoryEventBus @Inject constructor() : EventBus {
    private val stream = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    override val events: Flow<DomainEvent> = stream.asSharedFlow()
    override suspend fun publish(event: DomainEvent) {
        stream.emit(event)
    }
}
