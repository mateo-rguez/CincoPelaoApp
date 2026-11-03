package com.example.cincopelao

import kotlinx.serialization.Serializable

// Paquete de datos que se envía entre dispositivos al compartir.

@Serializable
data class DataToSend(
    val subjects: List<Subject> = emptyList(),
    val events: List<Event> = emptyList(),
    val tasks: List<Task> = emptyList()
)
