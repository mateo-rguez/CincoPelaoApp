package io.github.mateorguez.cincopelao.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideBleGattRepository(@ApplicationContext context: Context): BleGattRepository {
        return BleGattRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideWifiDirectRepository(@ApplicationContext context: Context): WifiDirectRepository {
        return WifiDirectRepositoryImpl(context)
    }
}