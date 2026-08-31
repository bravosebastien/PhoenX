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
     * MIGRATION_41_42 — Enrichissement Médiathèques v9.4.27 (Lot 4.1)
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

    /**
     * MIGRATION_42_43 — Personnalisation v9.4.27 (Lot 4.2)
     * Ajout du champ userComment et renommage de description.
     */
    val MIGRATION_42_43 = object : Migration(42, 43) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Extension offline_entries
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN userComment TEXT")

            // Renommage standalone_media
            db.execSQL("ALTER TABLE standalone_media RENAME COLUMN description TO userComment")
        }
    }

    /**
     * MIGRATION_43_44 — Le Miroir à Deux & Lien Vivant v9.4.27
     */
    val MIGRATION_43_44 = object : Migration(43, 44) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Extension table pacts (Le Miroir à Deux)
            db.execSQL("ALTER TABLE pacts ADD COLUMN partnerId TEXT")
            db.execSQL("ALTER TABLE pacts ADD COLUMN myStatus TEXT NOT NULL DEFAULT 'writing'")
            db.execSQL("ALTER TABLE pacts ADD COLUMN partnerStatus TEXT NOT NULL DEFAULT 'writing'")
            db.execSQL("ALTER TABLE pacts ADD COLUMN myConsentToBook INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE pacts ADD COLUMN partnerConsentToBook INTEGER NOT NULL DEFAULT 0")

            // 2. Création de la table living_links
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `living_links` (
                    `id` TEXT NOT NULL, 
                    `creatorId` TEXT NOT NULL, 
                    `recipientId` TEXT NOT NULL, 
                    `recipientName` TEXT NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `status` TEXT NOT NULL, 
                    `scheduledAt` INTEGER, 
                    `sentAt` INTEGER, 
                    `originalEntryId` TEXT, 
                    `syncStatus` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    /**
     * MIGRATION_44_45 — Tonalité libre v9.4.27
     */
    val MIGRATION_44_45 = object : Migration(44, 45) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN tonalNuance TEXT")
        }
    }

    /**
     * MIGRATION_45_46 — Ambiance de transmission v9.4.27
     */
    val MIGRATION_45_46 = object : Migration(45, 46) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE creator_profile ADD COLUMN transmissionBackgroundId TEXT DEFAULT 'PAPER_IVORY'")
            db.execSQL("ALTER TABLE creator_profile ADD COLUMN transmissionFontId TEXT DEFAULT 'MODERN'")
        }
    }

    /**
     * MIGRATION_46_47 — Ambiance PAR DESTINATAIRE v9.4.27
     * Déplacement des préférences du profil global vers chaque destinataire.
     */
    val MIGRATION_46_47 = object : Migration(46, 47) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Ajout des colonnes à la table recipients
            db.execSQL("ALTER TABLE recipients ADD COLUMN transmissionBackgroundId TEXT NOT NULL DEFAULT 'classic_ivory'")
            db.execSQL("ALTER TABLE recipients ADD COLUMN transmissionFontId TEXT NOT NULL DEFAULT 'playfair_display'")

            // 2. Nettoyage de creator_profile (Recreation car SQLite ne supporte pas DROP COLUMN)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `creator_profile_new` (
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

            db.execSQL("""
                INSERT INTO `creator_profile_new` (userId, bio, profession, hasSiblings, siblingsDetail, hasChildren, childrenDetail, hobbies, height, weight, eyeColor, hairColor, updatedAt, syncStatus)
                SELECT userId, bio, profession, hasSiblings, siblingsDetail, hasChildren, childrenDetail, hobbies, height, weight, eyeColor, hairColor, updatedAt, syncStatus FROM `creator_profile`
            """.trimIndent())

            db.execSQL("DROP TABLE `creator_profile`")
            db.execSQL("ALTER TABLE `creator_profile_new` RENAME TO `creator_profile`")
        }
    }

    /**
     * MIGRATION_47_48 — Retour à l'Ambiance GLOBALE v9.4.27
     * Réintroduction des colonnes dans creator_profile.
     */
    val MIGRATION_47_48 = object : Migration(47, 48) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE creator_profile ADD COLUMN transmissionBackgroundId TEXT NOT NULL DEFAULT 'classic_ivory'")
            db.execSQL("ALTER TABLE creator_profile ADD COLUMN transmissionFontId TEXT NOT NULL DEFAULT 'playfair_display'")
        }
    }

    /**
     * MIGRATION_48_49 — Photos de proches dans le livre v9.4.29
     */
    val MIGRATION_48_49 = object : Migration(48, 49) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE creator_profile ADD COLUMN showPersonPhotos INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recipients ADD COLUMN showPersonPhotos INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * MIGRATION_49_50 — Les Rencontres v9.5.0
     * Extension du modèle PersonEntity pour le nouveau tiroir.
     */
    val MIGRATION_49_50 = object : Migration(49, 50) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE persons ADD COLUMN categories TEXT NOT NULL DEFAULT ',FAMILY,'")
            db.execSQL("ALTER TABLE persons ADD COLUMN introducedById TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN encounterAge INTEGER")
            db.execSQL("ALTER TABLE persons ADD COLUMN encounterLocationId TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN encounterLocationLabel TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN linkNature TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN linkStatus TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN visibility TEXT NOT NULL DEFAULT 'PUBLIC'")
        }
    }

    /**
     * MIGRATION_50_51 — Refonte Galerie Rencontres v9.6.0
     * Ajout des champs de contexte et de fin de relation.
     */
    val MIGRATION_50_51 = object : Migration(50, 51) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE persons ADD COLUMN encounterContext TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN encounterContextLabel TEXT")
            db.execSQL("ALTER TABLE persons ADD COLUMN relationEndAge INTEGER")
            db.execSQL("ALTER TABLE persons ADD COLUMN relationEndReason TEXT")
        }
    }

    /**
     * MIGRATION_51_52 — Isolation des champs Rencontres v9.6.5
     */
    val MIGRATION_51_52 = object : Migration(51, 52) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Ajouter le champ spécifique à la bio des Rencontres
            db.execSQL("ALTER TABLE persons ADD COLUMN encounterBiography TEXT NOT NULL DEFAULT ''")
            
            // 2. Copier l'ancienne bio vers ce nouveau champ UNIQUEMENT pour les rencontres
            db.execSQL("UPDATE persons SET encounterBiography = biography WHERE categories LIKE '%ENCOUNTER%'")
            
            // 3. Nettoyer les faux contextes 'OTHER' créés par le bug précédent
            db.execSQL("UPDATE persons SET encounterContext = NULL WHERE encounterContext = 'OTHER'")
        }
    }

    /**
     * MIGRATION_52_53 — Nettoyage supplémentaire des contextes 'OTHER' (suite bug d'édition)
     */
    val MIGRATION_52_53 = object : Migration(52, 53) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE persons SET encounterContext = NULL WHERE encounterContext = 'OTHER'")
        }
    }

    /**
     * MIGRATION_53_54 — Photo de profil dédiée aux Rencontres
     */
    val MIGRATION_53_54 = object : Migration(53, 54) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE persons ADD COLUMN encounterImagePath TEXT")
        }
    }

    /**
     * MIGRATION_54_55 — Miniature pour vidéos de Rencontres (v9.6.6)
     */
    val MIGRATION_54_55 = object : Migration(54, 55) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE person_media ADD COLUMN thumbnailPath TEXT")
        }
    }

    /**
     * MIGRATION_55_56 — Réconciliation Sécurisée des Souvenirs (v9.6.7)
     */
    val MIGRATION_55_56 = object : Migration(55, 56) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN markedForDeletionAt INTEGER")
        }
    }

    /**
     * MIGRATION_56_57 — Contrôle granulaire des photos dans le Livre (v9.6.7)
     */
    val MIGRATION_56_57 = object : Migration(56, 57) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE offline_entries ADD COLUMN includedInBook INTEGER NOT NULL DEFAULT 1")
        }
    }
}
