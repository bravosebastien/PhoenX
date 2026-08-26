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
    NotificationContactEntity::class,
    PersonEntity::class,
    CreatorProfileEntity::class,
    StandaloneMediaEntity::class,
    PersonMediaEntity::class,
    LivingLinkEntity::class
], version = 50, exportSchema = false)
abstract class PhoenXDatabase : RoomDatabase() {
    abstract fun offlineEntryDao(): OfflineEntryDao
    abstract fun standaloneMediaDao(): StandaloneMediaDao
    abstract fun personMediaDao(): PersonMediaDao
}
