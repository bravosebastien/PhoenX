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

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Création de la table witnesses
            db.execSQL("CREATE TABLE IF NOT EXISTS `witnesses` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `status` TEXT NOT NULL, `submittedAt` INTEGER, `allowCreatorToRead` INTEGER NOT NULL, `allowCreatorToReject` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            // Création de la table notification_contacts
            db.execSQL("CREATE TABLE IF NOT EXISTS `notification_contacts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `relationship` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Vérification sécurisée pour éviter l'erreur "duplicate column"
            val cursor = db.query("PRAGMA table_info(offline_entries)")
            var columnExists = false
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex >= 0) {
                    val name = cursor.getString(nameIndex)
                    if (name == "parentEntryId") {
                        columnExists = true
                        break
                    }
                }
            }
            cursor.close()

            if (!columnExists) {
                db.execSQL("ALTER TABLE offline_entries ADD COLUMN parentEntryId TEXT")
            }
            
            // Migration de la visibilité
            db.execSQL("UPDATE offline_entries SET visibility = 'RESTRICTED' WHERE visibility = 'private'")
            db.execSQL("UPDATE offline_entries SET visibility = 'EVERYONE' WHERE visibility = 'public'")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE witnesses ADD COLUMN requestPrompt TEXT")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN creatorUid TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN enigmaHint TEXT")
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN enigmaAutoUnlockDays INTEGER")
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN questionId TEXT")
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN personIds TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE TABLE IF NOT EXISTS `persons` (`id` TEXT NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT, `relationship` TEXT, `distinctionType` TEXT, `distinctionValue` TEXT, `createdAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN isUltimateSecret INTEGER NOT NULL DEFAULT 0")
        }
    }
}
