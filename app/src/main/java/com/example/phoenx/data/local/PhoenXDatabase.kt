package com.example.phoenx.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [OfflineEntry::class, AmendmentEntity::class, PortraitEntity::class, FavoriteEntity::class, RecipientEntity::class, DepositaryEntity::class], version = 6, exportSchema = false)
abstract class PhoenXDatabase : RoomDatabase() {
    abstract fun offlineEntryDao(): OfflineEntryDao
}
