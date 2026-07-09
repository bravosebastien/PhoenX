package com.example.phoenx.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomMigrations {
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Ajout de la colonne locationId à la table offline_entries
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN locationId TEXT")
        }
    }
}
