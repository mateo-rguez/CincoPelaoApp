package io.github.mateorguez.cincopelao.data.repository

import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/** Eventos (exámenes, entregas...) de la instancia activa. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class EventRepository @Inject constructor(
    private val instances: InstanceRepository
) {
    private val db get() = instances.currentDb()

    val events: Flow<List<Event>> =
        instances.currentDatabase.flatMapLatest { it.eventDao().getAllEvents() }

    fun eventsForSubject(subjectId: Long): Flow<List<Event>> =
        instances.currentDatabase.flatMapLatest { it.eventDao().getEventsForSubject(subjectId) }

    suspend fun insert(event: Event) = db.eventDao().insertEvent(event)

    suspend fun insertAll(events: List<Event>) = db.eventDao().insertEvents(events)

    suspend fun update(event: Event) = db.eventDao().updateEvent(event)

    suspend fun delete(event: Event) = db.eventDao().deleteEvent(event)
}
