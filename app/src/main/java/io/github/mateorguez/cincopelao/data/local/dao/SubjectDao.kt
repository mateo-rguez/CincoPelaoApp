package io.github.mateorguez.cincopelao.data.local.dao

import androidx.room.*
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: Subject): Long

    @Update
    suspend fun update(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    suspend fun getAllSubjectsOnce(): List<Subject>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int

    @Query("SELECT * FROM subjects WHERE subjectId = :id")
    fun getSubjectById(id: Long): Flow<Subject?> // null si se ha borrado

    @Transaction
    suspend fun insertSubjectWithEvents(isEditing: Boolean, subject: Subject, events: List<Event>) {
        if (isEditing) {
            update(subject)
            deleteEventsForSubject(subject.subjectId)
            events.forEach { event ->
                insertEvent(event.copy(subjectId = subject.subjectId))
            }
        } else {
            val subjectId = insert(subject)
            events.forEach { event ->
                insertEvent(event.copy(subjectId = subjectId))
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Query("DELETE FROM events WHERE subjectId = :subjectId")
    suspend fun deleteEventsForSubject(subjectId: Long)

}