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
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_26_27,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_27_28,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_28_29,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_29_30,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_30_31,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_31_32,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_32_33,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_33_34,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_34_35,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_35_36,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_36_37,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_37_38,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_38_39,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_39_40,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_40_41,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_41_42,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_42_43,
            com.example.phoenx.data.local.RoomMigrations.MIGRATION_43_44
        ).build()
    }

    @Provides
    fun provideOfflineEntryDao(db: PhoenXDatabase): OfflineEntryDao {
        return db.offlineEntryDao()
    }

    @Provides
    fun provideStandaloneMediaDao(db: PhoenXDatabase): com.example.phoenx.data.local.StandaloneMediaDao {
        return db.standaloneMediaDao()
    }

    @Provides
    fun providePersonMediaDao(db: PhoenXDatabase): com.example.phoenx.data.local.PersonMediaDao {
        return db.personMediaDao()
    }
}
