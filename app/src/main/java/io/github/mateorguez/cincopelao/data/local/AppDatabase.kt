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

@Database(entities = [Subject::class, Event::class, Task::class], version = 8)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinco_pelao_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}