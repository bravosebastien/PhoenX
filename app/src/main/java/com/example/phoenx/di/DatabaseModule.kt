package com.example.phoenx.di

import android.content.Context
import androidx.room.Room
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PhoenXDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhoenXDatabase {
        return Room.databaseBuilder(
            context,
            PhoenXDatabase::class.java,
            "phoenx_db"
        ).addMigrations(
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_10_11,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_11_12,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_12_13,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_13_14,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_14_15,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_15_16,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_16_17,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_17_18,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_18_19,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_19_20,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_20_21,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_21_22,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_22_23,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_23_24,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_24_25,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_25_26,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_26_27
        ).build()
    }

    @Provides
    fun provideOfflineEntryDao(db: PhoenXDatabase): OfflineEntryDao {
        return db.offlineEntryDao()
    }
}
