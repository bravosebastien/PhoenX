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
        ).addMigrations(com.example.phoenx.data.local.RoomMigrations.MIGRATION_10_11)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideOfflineEntryDao(db: PhoenXDatabase): OfflineEntryDao {
        return db.offlineEntryDao()
    }
}
