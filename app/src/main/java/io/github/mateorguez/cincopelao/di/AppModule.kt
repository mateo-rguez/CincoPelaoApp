package io.github.mateorguez.cincopelao.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mateorguez.cincopelao.data.local.AppDatabase
import io.github.mateorguez.cincopelao.data.local.dao.EventDao
import io.github.mateorguez.cincopelao.data.local.dao.SubjectDao
import io.github.mateorguez.cincopelao.data.local.dao.TaskDao
import io.github.mateorguez.cincopelao.data.repository.RepositoryDB
import io.github.mateorguez.cincopelao.sharing.ble.BleGattRepository
import io.github.mateorguez.cincopelao.sharing.ble.BleGattRepositoryImpl
import io.github.mateorguez.cincopelao.sharing.wifidirect.WifiDirectRepository
import io.github.mateorguez.cincopelao.sharing.wifidirect.WifiDirectRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "cinco_pelao_db").build()
    }

    @Provides
    fun provideSubjectDao(db: AppDatabase): SubjectDao {
        return db.subjectDao()
    }

    @Provides
    fun provideEventDao(db: AppDatabase): EventDao {
        return db.eventDao()
    }

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao {
        return db.taskDao()
    }

    @Provides
    @Singleton
    fun provideRepository(subjectDao: SubjectDao, eventDao: EventDao, taskDao: TaskDao): RepositoryDB {
        return RepositoryDB(subjectDao, eventDao, taskDao)
    }

    @Provides
    @Singleton
    fun provideBleGattRepository(@ApplicationContext context: Context): BleGattRepository {
        return BleGattRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideWifiDirectRepository(@ApplicationContext context: Context): WifiDirectRepository {
        return WifiDirectRepositoryImpl(context)
    }
}