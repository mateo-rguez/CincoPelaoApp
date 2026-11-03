package io.github.mateorguez.cincopelao.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.mateorguez.cincopelao.data.local.entity.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)

    @Update
    suspend fun updateEvent(event: Event)

    @Query("SELECT * FROM events WHERE subjectId = :subjectId ORDER BY date ASC")
    fun getEventsForSubject(subjectId: Int): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE subjectId = :subjectId")
    suspend fun getEventsForSubjectOnce(subjectId: Int): List<Event>

    @Delete
    suspend fun deleteEvent(event: Event)
}