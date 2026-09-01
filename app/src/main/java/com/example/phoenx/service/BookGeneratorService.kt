package com.example.phoenx.service

import android.util.Log
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.model.*
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BookGeneratorService — Cœur de l'IA Biographe de PHOEN-X.
 * RÈGLE D'OR : Ce service ne manipule JAMAIS le texte brut chiffré Tink.
 * Il travaille exclusivement sur les résumés et tags générés localement par Gemini Nano.
 */
@Singleton
class BookGeneratorService @Inject constructor(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) {

    /**
     * Charge le brouillon de livre actuel depuis Firestore.
     */
    suspend fun loadBookDraft(userId: String): BookDraft? {
        android.util.Log.d("PHOENX_BOOK_TRACE", "1. Entrée loadBookDraft pour: $userId")
        
        val doc = db.collection("users").document(userId)
            .collection("book").document("current_draft").get().await()
        
        if (!doc.exists()) {
            android.util.Log.w("PHOENX_BOOK_TRACE", "2. Document current_draft INTROUVABLE.")
            return null
        }

        val rawData = doc.data ?: return null
        android.util.Log.d("PHOENX_BOOK_TRACE", "2. Document trouvé. Début mapping manuel (Zéro dépendance mapper automatique)")

        return try {
            // Helper de conversion sécurisée (Dates & Nombres)
            fun toLong(value: Any?, default: Long = System.currentTimeMillis()): Long {
                return when (value) {
                    is Long -> value
                    is com.google.firebase.Timestamp -> value.toDate().time
                    is Number -> value.toLong()
                    else -> default
                }
            }

            // 1. Mapping des Chapitres
            val chaptersRaw = rawData["chapters"] as? List<*>
            val chapters = chaptersRaw?.mapNotNull { item ->
                val ch = item as? Map<*, *> ?: return@mapNotNull null
                BookChapter(
                    id = ch["id"] as? String ?: "",
                    title = ch["title"] as? String ?: "Sans titre",
                    content = ch["content"] as? String ?: "",
                    status = try { ChapterStatus.valueOf(ch["status"] as? String ?: "DRAFT") } catch(e: Exception) { ChapterStatus.DRAFT },
                    lastModified = toLong(ch["lastModified"]),
                    orderIndex = (ch["orderIndex"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()

            // 2. Mapping du Thème
            val themeMap = rawData["theme"] as? Map<*, *>
            val theme = BookTheme(
                backgroundId = themeMap?.get("backgroundId") as? String ?: "classic_ivory",
                fontId = themeMap?.get("fontId") as? String ?: "eb_garamond"
            )

            // 3. Construction de l'objet final
            val draft = BookDraft(
                id = rawData["id"] as? String ?: "",
                userId = rawData["userId"] as? String ?: userId,
                generatedAt = toLong(rawData["generatedAt"]),
                lastUpdatedAt = toLong(rawData["lastUpdatedAt"]),
                status = try { BookStatus.valueOf(rawData["status"] as? String ?: "DRAFT") } catch(e: Exception) { BookStatus.DRAFT },
                chapters = chapters,
                totalEntries = (rawData["totalEntries"] as? Number)?.toInt() ?: 0,
                bookTitle = rawData["bookTitle"] as? String,
                recipientIds = (rawData["recipientIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                sealedMessage = rawData["sealedMessage"] as? String ?: "",
                globalIntroduction = rawData["globalIntroduction"] as? String ?: "",
                theme = theme,
                coverImageUrl = rawData["coverImageUrl"] as? String,
                coverTitleStyle = rawData["coverTitleStyle"] as? String ?: "GOLD",
                visibility = rawData["visibility"] as? String ?: "RESTRICTED",
                coverScale = (rawData["coverScale"] as? Number)?.toFloat() ?: 1f,
                coverOffsetX = (rawData["coverOffsetX"] as? Number)?.toFloat() ?: 0f,
                coverOffsetY = (rawData["coverOffsetY"] as? Number)?.toFloat() ?: 0f,
                coverUploadedAt = toLong(rawData["coverUploadedAt"], 0L).let { if (it == 0L) null else it }
            )

            android.util.Log.d("PHOENX_BOOK_TRACE", "3. Mapping manuel réussi. Chapitres: ${draft.chapters.size}")
            draft
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK_TRACE", "ERREUR FATALE lors du mapping manuel", e)
            null
        }
    }

    suspend fun getBookKey(userId: String): ByteArray? {
        return try {
            val keyDoc = db.collection("users").document(userId)
                .collection("book_keys").document("main").get().await()
            val keyBase64 = keyDoc.getString("key")
            if (keyBase64 != null) {
                android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun decryptChapter(encryptedBase64: String, bookKey: ByteArray?): String {
        return try {
            val bytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
            encryptionManager.decryptText(bytes, bookKey)
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK", "Déchiffrement chapitre échoué", e)
            encryptedBase64 // Retourne le code si échec pour diagnostic
        }
    }

    fun encryptChapter(plainText: String, bookKey: ByteArray?): String {
        return try {
            val encrypted = encryptionManager.encryptText(plainText, bookKey)
            android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK", "Chiffrement chapitre échoué", e)
            plainText
        }
    }

    /**
     * Extrait les "Scènes de Vie" (Souvenirs groupés avec leurs médias)
     */
    suspend fun extractScenes(): List<Map<String, Any?>> {
        val allEntries = offlineEntryDao.getAllEntriesSync()
        val allPersons = offlineEntryDao.getAllPersons().first()
        val personMap = allPersons.associateBy { it.id }
        
        // Cache des pactes pour éviter des requêtes répétitives (v9.4.27)
        val pacts = offlineEntryDao.getAllPacts().first().associateBy { it.id }

        // v9.4.27 : Filtrage strict
        val parents = allEntries.filter { entry ->
            val isParent = entry.parentEntryId == null
            val isIncluded = entry.includeInBook
            val noEnigma = entry.enigmaQuestion == null
            
            val doubleConsentOk = if (entry.pactId != null) {
                val pact = pacts[entry.pactId]
                pact != null && pact.myConsentToBook && pact.partnerConsentToBook
            } else true

            val accepted = isParent && isIncluded && noEnigma && doubleConsentOk
            
            if (!accepted) {
                val reason = when {
                    !isParent -> "Complément (parent=${entry.parentEntryId})"
                    !isIncluded -> "Exclu par l'utilisateur (includeInBook=false)"
                    !noEnigma -> "Protégé par énigme"
                    !doubleConsentOk -> "Attente double consentement (Pacte)"
                    else -> "Inconnu"
                }
                android.util.Log.d("PHOENX_BOOK_DEBUG", "REJETÉ: ID=${entry.id}, Title=${entry.aiSummary}, Raison=$reason")
            }
            
            accepted
        }
        
        return parents.map { parent ->
            val complements = allEntries.filter { it.parentEntryId == parent.id }
            val age = AgeUtils.parseAgeJson(parent.ageAtCreation)
            
            // v9.4.27 : Priority 1 - Amendments (Evolution of thought)
            val amendments = offlineEntryDao.getAmendmentsForEntrySync(parent.id)
            val mappedAmendments = amendments.mapNotNull { am ->
                if (!am.aiEvolution.isNullOrBlank()) {
                    mapOf(
                        "age" to AgeUtils.parseAgeJson(am.ageAtAmendment).years,
                        "evolution" to am.aiEvolution
                    )
                } else null
            }
            
            // v9.0 : Résolution des personnages cités dans cette scène
            val taggedIds = parent.personIds.split(",").filter { it.isNotBlank() }.map { it.trim() }
            val characters = taggedIds.mapNotNull { id ->
                personMap[id]?.let { p ->
                    mapOf(
                        "firstName" to p.firstName,
                        "lastName" to p.lastName,
                        "relationship" to p.relationship,
                        "distinctionType" to p.distinctionType,
                        "distinctionValue" to p.distinctionValue,
                        // Nouveaux champs v9.0
                        "height" to p.height,
                        "weight" to p.weight,
                        "eyeColor" to p.eyeColor,
                        "hairColor" to p.hairColor,
                        "clothingStyle" to p.clothingStyle,
                        "profession" to p.profession,
                        "hasChildren" to p.hasChildren,
                        "relationshipDetail" to p.relationshipDetail,
                        // v9.4.27 : Priority 3 - Genealogy Context
                        "parentIds" to p.parentIds,
                        "biography" to p.biography
                    )
                }
            }
            
            val originType = when {
                parent.entryType == "PORTRAIT" -> "PORTRAIT"
                parent.questionId != null -> "QUESTION"
                else -> "MEMORY"
            }

            mapOf(
                "id" to parent.id, // v9.3.1
                "summary" to parent.aiSummary,
                "age" to age.years,
                "category" to parent.emotionalCategory,
                "tonalNuance" to parent.tonalNuance, // Ajouté v9.4.27
                "soulTone" to parent.soulTone,
                "originType" to originType, // Injection v9.3.1
                "characters" to characters, // Transmis à l'IA Biographe
                "userComment" to parent.userComment, // Priority 2 : Personal context
                "amendments" to mappedAmendments, // Priority 1 : Thought evolution
                "photos" to complements.filter { it.entryType == "PHOTO" || it.entryType == "GALLERY" }
                    .map { mapOf("id" to it.id, "description" to it.aiSummary, "userComment" to it.userComment) },
                "vocal_essence" to complements.filter { it.entryType == "AUDIO" }
                    .map { mapOf("id" to it.id, "description" to it.aiSummary, "userComment" to it.userComment) },
                "stories" to complements.filter { it.entryType == "TEXT" || it.entryType == "THOUGHT" }
                    .map { mapOf("id" to it.id, "description" to it.aiSummary, "userComment" to it.userComment) }
            )
        }.sortedBy { it["age"] as Int }
    }

    /**
     * Propose un plan de livre à partir des souvenirs (v9.3.1)
     */
    suspend fun generateBookPlan(): List<Map<String, Any?>> {
        val scenes = extractScenes()
        if (scenes.isEmpty()) throw Exception("Pas assez de souvenirs.")

        val data = hashMapOf("scenes" to scenes)
        val result = functions.getHttpsCallable("generateBookPlan")
            .call(data)
            .await()

        val response = result.data as Map<*, *>
        @Suppress("UNCHECKED_CAST")
        return response["plan"] as List<Map<String, Any?>>
    }

    /**
     * Lance la génération complète du manuscrit multimédia.
     * v9.3.1 : Support optionnel d'un plan validé.
     */
    suspend fun generateBook(
        userId: String, 
        plan: List<Map<String, Any?>>? = null,
        onProgress: (String) -> Unit
    ): BookDraft {
        onProgress("Préparation des scènes de ta vie...")
        
        // v9.4.29 : Récupération du draft actuel pour PRÉSERVER les métadonnées (Titre, Couverture, Thème)
        val existingDraft = loadBookDraft(userId)
        
        val scenes = extractScenes()
        
        // v9.4.29 : Log des scènes envoyées à l'IA pour diagnostic "réunion"
        try {
            val sceneSummary = scenes.map { s -> 
                "ID: ${s["id"]}, Title: ${s["summary"]}, Age: ${s["age"]}, Included: true"
            }.joinToString("\n")
            android.util.Log.d("PHOENX_BOOK_DEBUG", "SCÈNES ENVOYÉES À L'IA :\n$sceneSummary")
        } catch (e: Exception) {
            android.util.Log.w("PHOENX_BOOK_DEBUG", "Échec log résumé scènes")
        }
        
        if (scenes.isEmpty()) {
            throw Exception("Pas assez de souvenirs pour écrire ton livre.")
        }
        
        // ... (reste de la logique de préparation inchangée) ...
        
        // v9.1 : Récupération du profil enrichi du Créateur
        val richProfile = offlineEntryDao.getCreatorProfileSync(userId)
        val authorProfileMap = richProfile?.let { p ->
            val map = mutableMapOf<String, Any>()
            if (!p.bio.isNullOrBlank()) map["bio"] = p.bio
            if (!p.profession.isNullOrBlank()) map["profession"] = p.profession
            if (p.hasSiblings == true && !p.siblingsDetail.isNullOrBlank()) map["family_siblings"] = p.siblingsDetail
            if (p.hasChildren == true && !p.childrenDetail.isNullOrBlank()) map["family_children"] = p.childrenDetail
            if (!p.hobbies.isNullOrBlank()) map["hobbies"] = p.hobbies
            // Portrait physique
            val physical = mutableListOf<String>()
            p.height?.let { physical.add("$it cm") }
            p.weight?.let { physical.add("$it kg") }
            p.eyeColor?.let { physical.add("Yeux $it") }
            p.hairColor?.let { physical.add("Cheveux $it") }
            if (physical.isNotEmpty()) map["physical_appearance"] = physical.joinToString(", ")
            
            if (map.isNotEmpty()) map else null
        }

        // v9.3.1 : Calcul du Ton de l'Âme dominant
        val allTones = scenes.mapNotNull { it["soulTone"] as? String }
        val dominantTone = allTones.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        onProgress("Analyse de l'évolution de ta pensée...")
        val evolutionInsights = try {
            val summaries = scenes.mapNotNull { it["summary"] as? String }
            if (summaries.isNotEmpty()) {
                val evolutionResult = functions.getHttpsCallable("detectThoughtEvolution")
                    .call(hashMapOf("summaries" to summaries))
                    .await()
                evolutionResult.data as? String
            } else null
        } catch (e: Exception) {
            null
        }

        onProgress("Rédaction des chapitres illustrés par l'IA...")
        
        val ageMin = scenes.minOf { it["age"] as Int }
        val ageMax = scenes.maxOf { it["age"] as Int }

        val data = hashMapOf(
            "scenes" to scenes,
            "ageMin" to ageMin,
            "ageMax" to ageMax,
            "soulTone" to dominantTone, // Injection v9.3.1
            "authorProfile" to authorProfileMap, // Transmis à l'IA Biographe v9.1
            "plan" to plan, // Injection v9.3.1
            "evolutionInsights" to evolutionInsights // Injection v9.3.1
        )

        // v9.0 : Log temporaire du payload envoyé à l'IA pour vérification des fiches personnages
        try {
            val json = org.json.JSONObject(data as Map<*, *>).toString(2)
            android.util.Log.d("PHOENX_AI_PROMPT", "Payload envoyé à generateBookChapters :\n$json")
        } catch (e: Exception) {
            android.util.Log.w("PHOENX_AI_PROMPT", "Impossible de logguer le payload JSON")
        }

        val result = functions.getHttpsCallable("generateBookChapters")
            .call(data)
            .await()

        val response = result.data as Map<*, *>
        val rawChapters = response["chapters"] as List<Map<*, *>>

        onProgress("Chiffrement et sécurisation...")

        // v9.7.4 : SAUVEGARDE AUTOMATIQUE AVANT RÉÉCRITURE
        try {
            backupCurrentDraft(userId)
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK", "Échec sauvegarde backup avant régénération", e)
        }

        // 1. GÉNÉRATION D'UNE CLÉ DÉDIÉE AU LIVRE (Pour transmission future)
        val bookKey = encryptionManager.generateNewSessionKey()
        val bookKeyBase64 = android.util.Base64.encodeToString(bookKey, android.util.Base64.NO_WRAP)

        val chapters = rawChapters.map { ch ->
            val content = ch["content"] as String
            BookChapter(
                id = java.util.UUID.randomUUID().toString(),
                title = ch["title"] as String,
                content = encryptionManager.encryptText(content, bookKey).let { 
                    android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) 
                },
                status = ChapterStatus.DRAFT,
                orderIndex = (ch["orderIndex"] as Number).toInt()
            )
        }

        // v9.4.29 : On crée le nouveau draft en INJECTANT les métadonnées existantes
        val draft = BookDraft(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            chapters = chapters,
            totalEntries = scenes.size,
            bookTitle = existingDraft?.bookTitle,
            coverImageUrl = existingDraft?.coverImageUrl,
            coverTitleStyle = existingDraft?.coverTitleStyle ?: "GOLD",
            coverScale = existingDraft?.coverScale ?: 1f,
            coverOffsetX = existingDraft?.coverOffsetX ?: 0f,
            coverOffsetY = existingDraft?.coverOffsetY ?: 0f,
            coverUploadedAt = existingDraft?.coverUploadedAt,
            theme = existingDraft?.theme ?: BookTheme(),
            visibility = existingDraft?.visibility ?: "RESTRICTED",
            sealedMessage = existingDraft?.sealedMessage ?: ""
        )

        onProgress("Sauvegarde finale...")
        saveBookDraft(userId, draft)
        
        // Sauvegarde de la clé du livre
        db.collection("users").document(userId)
            .collection("book_keys").document("main")
            .set(mapOf("key" to bookKeyBase64))
            .await()

        return draft
    }

    suspend fun saveBookDraft(userId: String, draft: BookDraft) {
        try {
            val chaptersMap = draft.chapters.map { chapter ->
                mapOf(
                    "id" to chapter.id,
                    "title" to chapter.title,
                    "content" to chapter.content,
                    "status" to chapter.status.name,
                    "lastModified" to chapter.lastModified,
                    "orderIndex" to chapter.orderIndex
                )
            }

            // v9.4.29 : Filtrage systématique des valeurs NULL pour éviter l'effacement accidentel de champs Firestore
            val rawData: Map<String, Any?> = hashMapOf(
                "id" to draft.id,
                "userId" to draft.userId,
                "generatedAt" to draft.generatedAt,
                "lastUpdatedAt" to System.currentTimeMillis(),
                "status" to draft.status.name,
                "chapters" to chaptersMap,
                "totalEntries" to draft.totalEntries,
                "bookTitle" to draft.bookTitle,
                "recipientIds" to draft.recipientIds,
                "sealedMessage" to draft.sealedMessage,
                "globalIntroduction" to draft.globalIntroduction,
                "theme" to mapOf(
                    "backgroundId" to draft.theme.backgroundId,
                    "fontId" to draft.theme.fontId
                ),
                "coverImageUrl" to draft.coverImageUrl,
                "coverTitleStyle" to draft.coverTitleStyle,
                "visibility" to (draft.visibility ?: "RESTRICTED"),
                "coverScale" to draft.coverScale,
                "coverOffsetX" to draft.coverOffsetX,
                "coverOffsetY" to draft.coverOffsetY,
                "coverUploadedAt" to draft.coverUploadedAt
            )
            
            val filteredData = rawData.filterValues { it != null }

            Log.d("PHOENX_COVER_W", "Écriture Firestore: doc=users/$userId/book/current_draft, data=$filteredData")
            db.collection("users").document(userId)
                .collection("book").document("current_draft")
                .set(filteredData, com.google.firebase.firestore.SetOptions.merge())
                .await()
            Log.d("PHOENX_COVER_W", "Écriture RÉUSSIE")
        } catch (e: Exception) {
            Log.e("PHOENX_COVER_W", "ERREUR ÉCRITURE: ${e.message}")
            android.util.Log.e("PHOENX_BOOK", "Erreur lors de la sauvegarde: ${e.message}")
            throw e // Renvoyer l'erreur pour que l'UI puisse l'afficher
        }
    }

    /**
     * Permet à l'auteur de modifier un chapitre en dialoguant avec l'IA.
     */
    suspend fun askAiToModifyChapter(currentContent: String, instruction: String): String {
        val data = hashMapOf(
            "currentContent" to currentContent,
            "instruction" to instruction
        )
        
        val result = functions.getHttpsCallable("modifyBookChapter")
            .call(data)
            .await()
            
        val response = result.data as Map<*, *>
        return response["newContent"] as String
    }

    /**
     * Génère une introduction globale pour le livre entier (v8.7.0).
     */
    suspend fun generateGlobalIntro(chapterTitles: List<String>): String {
        val data = hashMapOf("chapterTitles" to chapterTitles)
        val result = functions.getHttpsCallable("generateGlobalIntro")
            .call(data)
            .await()
        val response = result.data as Map<*, *>
        return response["content"] as String
    }

    // --- v9.7.4 : SÉCURITÉ RÉGÉNÉRATION (BACKUP & RESTORE) ---

    suspend fun hasBackup(userId: String): Boolean {
        return try {
            val doc = db.collection("users").document(userId)
                .collection("book").document("backup_draft").get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun backupCurrentDraft(userId: String) {
        val currentDoc = db.collection("users").document(userId)
            .collection("book").document("current_draft").get().await()
        
        if (currentDoc.exists()) {
            val data = currentDoc.data ?: return
            db.collection("users").document(userId)
                .collection("book").document("backup_draft")
                .set(data)
                .await()
            android.util.Log.d("PHOENX_BOOK", "Backup du manuscrit actuel créé.")
        }
    }

    suspend fun restoreFromBackup(userId: String) {
        val backupDoc = db.collection("users").document(userId)
            .collection("book").document("backup_draft").get().await()
        
        if (backupDoc.exists()) {
            val data = backupDoc.data ?: return
            // 1. Restaurer vers current_draft
            db.collection("users").document(userId)
                .collection("book").document("current_draft")
                .set(data)
                .await()
            
            // 2. Supprimer le backup
            db.collection("users").document(userId)
                .collection("book").document("backup_draft")
                .delete()
                .await()
            
            android.util.Log.d("PHOENX_BOOK", "Restauration depuis backup réussie.")
        } else {
            throw Exception("Aucune sauvegarde disponible à restaurer.")
        }
    }
}
