package io.github.mateorguez.cincopelao.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.mateorguez.cincopelao.data.local.dao.EventDao
import io.github.mateorguez.cincopelao.data.local.dao.SubjectDao
import io.github.mateorguez.cincopelao.data.local.dao.TaskDao
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task

/**
 * Base de datos de una instancia. Cada instancia tiene su propio archivo,
 * cuyo nombre es el id de la instancia (ver InstanceRepository).
 */
@Database(entities = [Subject::class, Event::class, Task::class], version = 8)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao

    companion object {
        /** Construye la base de datos. Room no abre el archivo hasta la primera consulta. */
        fun build(context: Context, fileName: String): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, fileName).build()
    }
}
