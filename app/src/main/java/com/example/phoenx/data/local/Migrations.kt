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

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Ajout de la colonne compartmentIds à la table offline_entries
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN compartmentIds TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Pipeline Média (Réservé)
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN mediaUrl TEXT")
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN localMediaPath TEXT")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Édition Avancée
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN memoryDate INTEGER")
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Période (Date début / Date fin)
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN memoryDateStart INTEGER")
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN memoryDateEnd INTEGER")
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE offline_entries SET visibility = 'private' WHERE visibility = 'Privé'")
        }
    }
}
