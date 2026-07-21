package com.example.phoenx.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    OfflineEntry::class, 
    AmendmentEntity::class, 
    PortraitEntity::class, 
    FavoriteEntity::class, 
    RecipientEntity::class, 
    DepositaryEntity::class, 
    LegacyEntity::class, 
    PactEntity::class,
    WitnessEntity::class,
    NotificationContactEntity::class
], version = 22, exportSchema = false)
abstract class PhoenXDatabase : RoomDatabase() {
    abstract fun offlineEntryDao(): OfflineEntryDao
}
