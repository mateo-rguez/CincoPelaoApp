package io.github.mateorguez.cincopelao.sharing.protocol

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import kotlinx.serialization.Serializable

// Paquete de datos que se envía entre dispositivos al compartir.

@Serializable
data class DataToSend(
    val subjects: List<Subject> = emptyList(),
    val events: List<Event> = emptyList(),
    val tasks: List<Task> = emptyList()
)
