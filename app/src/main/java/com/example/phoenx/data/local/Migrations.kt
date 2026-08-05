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

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN silentAttribution INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recipients ADD COLUMN phone TEXT")
        }
    }

    /**
     * MIGRATION_25_26 — Correction du schéma "Poison" (v8.9.8)
     * Supprime les clauses DEFAULT '' injectées historiquement via ALTER TABLE
     * qui faisaient échouer la validation de schéma de Room sur les anciens comptes.
     */
    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Création de la table temporaire sans AUCUN DEFAULT
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `offline_entries_new` (
                    `id` TEXT NOT NULL, 
                    `creatorUid` TEXT NOT NULL, 
                    `encryptedPayload` BLOB NOT NULL, 
                    `entryType` TEXT NOT NULL, 
                    `ageAtCreation` TEXT NOT NULL, 
                    `emotionalCategory` TEXT NOT NULL, 
                    `visibility` TEXT NOT NULL, 
                    `recipientIds` TEXT NOT NULL, 
                    `isYoungSelfLetter` INTEGER NOT NULL, 
                    `targetAge` INTEGER, 
                    `createdAt` INTEGER NOT NULL, 
                    `syncStatus` TEXT NOT NULL, 
                    `aiSummary` TEXT NOT NULL, 
                    `aiTags` TEXT NOT NULL, 
                    `enigmaQuestion` TEXT, 
                    `enigmaAnswer` TEXT, 
                    `scheduledTimestamp` INTEGER, 
                    `unlockAfterDays` INTEGER NOT NULL, 
                    `unlockedAt` INTEGER, 
                    `fallbackAnswer` TEXT, 
                    `latitude` REAL, 
                    `longitude` REAL, 
                    `locationName` TEXT, 
                    `pactId` TEXT, 
                    `locationId` TEXT, 
                    `compartmentIds` TEXT NOT NULL, 
                    `mediaUrl` TEXT, 
                    `localMediaPath` TEXT, 
                    `memoryDate` INTEGER, 
                    `memoryDateStart` INTEGER, 
                    `memoryDateEnd` INTEGER, 
                    `parentEntryId` TEXT, 
                    `enigmaHint` TEXT, 
                    `enigmaAutoUnlockDays` INTEGER, 
                    `questionId` TEXT, 
                    `personIds` TEXT NOT NULL, 
                    `isUltimateSecret` INTEGER NOT NULL, 
                    `silentAttribution` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            // 2. Copie des données
            db.execSQL("""
                INSERT INTO `offline_entries_new` (
                    id, creatorUid, encryptedPayload, entryType, ageAtCreation, emotionalCategory,
                    visibility, recipientIds, isYoungSelfLetter, targetAge, createdAt, syncStatus,
                    aiSummary, aiTags, enigmaQuestion, enigmaAnswer, scheduledTimestamp,
                    unlockAfterDays, unlockedAt, fallbackAnswer, latitude, longitude,
                    locationName, pactId, locationId, compartmentIds, mediaUrl, localMediaPath,
                    memoryDate, memoryDateStart, memoryDateEnd, parentEntryId, enigmaHint,
                    enigmaAutoUnlockDays, questionId, personIds, isUltimateSecret, silentAttribution
                ) SELECT 
                    id, creatorUid, encryptedPayload, entryType, ageAtCreation, emotionalCategory,
                    visibility, recipientIds, isYoungSelfLetter, targetAge, createdAt, syncStatus,
                    aiSummary, aiTags, enigmaQuestion, enigmaAnswer, scheduledTimestamp,
                    unlockAfterDays, unlockedAt, fallbackAnswer, latitude, longitude,
                    locationName, pactId, locationId, compartmentIds, mediaUrl, localMediaPath,
                    memoryDate, memoryDateStart, memoryDateEnd, parentEntryId, enigmaHint,
                    enigmaAutoUnlockDays, questionId, personIds, isUltimateSecret, silentAttribution
                FROM `offline_entries`
            """.trimIndent())

            // 3. Remplacement de la table
            db.execSQL("DROP TABLE `offline_entries`")
            db.execSQL("ALTER TABLE `offline_entries_new` RENAME TO `offline_entries`")
        }
    }

    /**
     * MIGRATION_26_27 — Rétablissement des index vitaux (Lot de Stabilisation v8.9.9)
     * Recrée les index sur offline_entries et amendments pour supprimer les figements
     * UI provoqués par des scans de table intégraux.
     */
    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Index pour offline_entries
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_createdAt` ON `offline_entries` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_parentEntryId` ON `offline_entries` (`parentEntryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_creatorUid` ON `offline_entries` (`creatorUid`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_pactId` ON `offline_entries` (`pactId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_locationId` ON `offline_entries` (`locationId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_entryType` ON `offline_entries` (`entryType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_scheduledTimestamp` ON `offline_entries` (`scheduledTimestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_entries_recipientIds` ON `offline_entries` (`recipientIds`)")

            // Index pour amendments
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_amendments_entryId` ON `amendments` (`entryId`)")
        }
    }

    /**
     * MIGRATION_27_28 — Lot v8.9.9b
     * Ajout des champs Cameo (imagePath), Souveraineté (includeInBook) et IA Narrative (soulTone).
     */
    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. imagePath (PersonEntity) : Nullable, pas de DEFAULT requis
            db.execSQL("ALTER TABLE persons ADD COLUMN imagePath TEXT")

            // 2. soulTone (OfflineEntry) : Nullable, pas de DEFAULT requis
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN soulTone TEXT")

            // 3. includeInBook (OfflineEntry) : NOT NULL avec DEFAULT 1 (true)
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN includeInBook INTEGER NOT NULL DEFAULT 1")
        }
    }

    /**
     * MIGRATION_28_29 — Écran Personnages v9.0
     * Extension du modèle PersonEntity pour le Livre de Vie.
     */
    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE persons ADD COLUMN height INTEGER")
            db.execSQL("ALTER TABLE persons ADD COLUMN weight INTEGER")
            db.execSQL("ALTER TABLE persons ADD COLUMN eyeColor TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN hairColor TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN clothingStyle TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN profession TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN hasChildren INTEGER")
            db.execSQL("ALTER TABLE persons ADD COLUMN relationshipDetail TEXT")
        }
    }

    /**
     * MIGRATION_29_30 — Profil Créateur v9.1
     * Création de la table creator_profile pour enrichir le Livre de Vie.
     */
    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `creator_profile` (
                    `userId` TEXT NOT NULL, 
                    `bio` TEXT, 
                    `profession` TEXT, 
                    `hasSiblings` INTEGER, 
                    `siblingsDetail` TEXT, 
                    `hasChildren` INTEGER, 
                    `childrenDetail` TEXT, 
                    `hobbies` TEXT, 
                    `height` INTEGER, 
                    `weight` INTEGER, 
                    `eyeColor` TEXT, 
                    `hairColor` TEXT, 
                    `updatedAt` INTEGER NOT NULL, 
                    `syncStatus` TEXT NOT NULL, 
                    PRIMARY KEY(`userId`)
                )
            """.trimIndent())
        }
    }

    /**
     * MIGRATION_30_31 — Distinction Humain / Animal v9.1
     */
    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE persons ADD COLUMN characterType TEXT")
        }
    }

    /**
     * MIGRATION_31_32 — Finalisation Dépositaires v9.1
     */
    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE depositaries ADD COLUMN linkedUid TEXT")
            db.execSQL("ALTER TABLE depositaries ADD COLUMN role TEXT")
        }
    }

    /**
     * MIGRATION_32_33 — Correction contrainte NOT NULL sur table depositaries
     */
    val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Création de la nouvelle table avec les contraintes exactes attendues par Room
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `depositaries_new` (
                    `id` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `email` TEXT NOT NULL, 
                    `phone` TEXT, 
                    `role` TEXT, 
                    `status` TEXT NOT NULL, 
                    `linkedUid` TEXT, 
                    `createdAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            // 2. Copie des données existantes
            db.execSQL("""
                INSERT INTO `depositaries_new` (id, name, email, phone, role, status, linkedUid, createdAt)
                SELECT id, name, email, phone, role, status, linkedUid, createdAt FROM `depositaries`
            """.trimIndent())

            // 3. Remplacement
            db.execSQL("DROP TABLE `depositaries`")
            db.execSQL("ALTER TABLE `depositaries_new` RENAME TO `depositaries`")
        }
    }

    /**
     * MIGRATION_33_34 — Support Heritage v9.2
     * Ajout du champ linkedUid à RecipientEntity pour les contrôles de sécurité.
     */
    val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recipients ADD COLUMN linkedUid TEXT")
        }
    }

    /**
     * MIGRATION_34_35 — Photos de Profil du Cercle v9.2.2
     */
    val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recipients ADD COLUMN photoUrl TEXT")
            db.execSQL("ALTER TABLE witnesses ADD COLUMN photoUrl TEXT")
            db.execSQL("ALTER TABLE depositaries ADD COLUMN photoUrl TEXT")
        }
    }

    /**
     * MIGRATION_35_36 — Bibliothèque Littéraire & Dépôts Directs v9.3.2
     */
    val MIGRATION_35_36 = object : Migration(35, 36) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `standalone_media` (
                    `id` TEXT NOT NULL, 
                    `creatorUid` TEXT NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `content` TEXT NOT NULL, 
                    `recipientIds` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    `syncStatus` TEXT NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_standalone_media_createdAt` ON `standalone_media` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_standalone_media_type` ON `standalone_media` (`type`)")
        }
    }

    /**
     * MIGRATION_36_37 — Ajout description StandaloneMedia v9.3.3
     */
    val MIGRATION_36_37 = object : Migration(36, 37) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE standalone_media ADD COLUMN description TEXT")
        }
    }

    /**
     * MIGRATION_37_38 — Ajout visibilité StandaloneMedia v9.4.19
     */
    val MIGRATION_37_38 = object : Migration(37, 38) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE standalone_media ADD COLUMN visibility TEXT NOT NULL DEFAULT 'RESTRICTED'")
        }
    }

    /**
     * MIGRATION_38_39 — Arbre Généalogique v9.4.22
     * 1. Extension PersonEntity (parentIds, isDeceased, biography)
     * 2. Création de la table person_media pour les galeries dédiées.
     */
    val MIGRATION_38_39 = object : Migration(38, 39) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Extension PersonEntity
            db.execSQL("ALTER TABLE persons ADD COLUMN parentIds TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE persons ADD COLUMN isDeceased INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE persons ADD COLUMN biography TEXT NOT NULL DEFAULT ''")

            // Création de la table person_media
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `person_media` (
                    `id` TEXT NOT NULL, 
                    `personId` TEXT NOT NULL, 
                    `mediaPath` TEXT NOT NULL, 
                    `mediaType` TEXT NOT NULL, 
                    `capturedAt` INTEGER NOT NULL, 
                    `syncStatus` TEXT NOT NULL, 
                    PRIMARY KEY(`id`), 
                    FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_person_media_personId` ON `person_media` (`personId`)")
        }
    }

    /**
     * MIGRATION_39_40 — Arbre Généalogique v9.4.23
     * Ajout du champ isReparented à PersonEntity.
     */
    val MIGRATION_39_40 = object : Migration(39, 40) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE persons ADD COLUMN isReparented INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * MIGRATION_40_41 — Arbre Généalogique v9.4.23
     * Ajout du champ reparentedRelationLabel à PersonEntity.
     */
    val MIGRATION_40_41 = object : Migration(40, 41) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE persons ADD COLUMN reparentedRelationLabel TEXT")
        }
    }

    /**
     * MIGRATION_41_42 — Enrichissement Médiathèques v9.4.27
     * Ajout des champs coverUrl, localCoverPath et mediaProvider.
     */
    val MIGRATION_41_42 = object : Migration(41, 42) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Extension offline_entries
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN coverUrl TEXT")
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN localCoverPath TEXT")
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN mediaProvider TEXT")

            // Extension standalone_media
            db.execSQL("ALTER TABLE standalone_media ADD COLUMN coverUrl TEXT")
            db.execSQL("ALTER TABLE standalone_media ADD COLUMN localCoverPath TEXT")
            db.execSQL("ALTER TABLE standalone_media ADD COLUMN mediaProvider TEXT")
        }
    }
}
