package com.example.phoenx.service

import android.util.Log
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.CreatorProfileEntity
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
                    orderIndex = (ch["orderIndex"] as? Number)?.toInt() ?: 0,
                    sceneIds = (ch["sceneIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    sourceFingerprint = ch["sourceFingerprint"] as? String ?: "",
                    sceneDiagnostics = (ch["sceneDiagnostics"] as? Map<*, *>)?.mapNotNull { (key, value) ->
                        val sceneId = key as? String ?: return@mapNotNull null
                        val diagMap = value as? Map<*, *> ?: return@mapNotNull null
                        sceneId to SceneDiagnostic(
                            contentHash = diagMap["contentHash"] as? String ?: "",
                            characterHashes = (diagMap["characterHashes"] as? Map<*, *>)
                                ?.entries?.mapNotNull { (k, v) -> (k as? String)?.let { it to (v as? String ?: "") } }
                                ?.toMap() ?: emptyMap(),
                            complementsHash = diagMap["complementsHash"] as? String ?: "",
                            photoCount = (diagMap["photoCount"] as? Number)?.toInt() ?: 0,
                            audioCount = (diagMap["audioCount"] as? Number)?.toInt() ?: 0,
                            storyCount = (diagMap["storyCount"] as? Number)?.toInt() ?: 0
                        )
                    }?.toMap() ?: emptyMap()
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
                coverUploadedAt = toLong(rawData["coverUploadedAt"], 0L).let { if (it == 0L) null else it },
                metaFingerprint = rawData["metaFingerprint"] as? String ?: ""
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

        // Chantier fusion des compléments (4 septembre) : format lisible pour l'IA (un horodatage
        // brut en millisecondes n'est pas fiable pour un LLM). Fuseau LOCAL de l'appareil,
        // explicitement, jamais UTC — une date saisie le 1er janvier ne doit jamais ressortir au
        // 31 décembre précédent.
        val eventDateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE).apply {
            timeZone = java.util.TimeZone.getDefault()
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

            // Étape 3 : l'IA du Livre lit désormais le Récit réel (déchiffré côté Créateur, à la
            // volée, jamais stocké en clair), et non plus le seul titre (aiSummary). Repli sur le
            // titre utilisateur si le Récit est vide ou son déchiffrement a échoué — jamais le texte
            // chiffré brut envoyé tel quel.
            val decryptedRecit = try {
                val text = encryptionManager.decryptText(parent.encryptedPayload)
                when {
                    text.isBlank() -> null
                    text == "Contenu chiffré" -> null // même sentinel que MemoryDetailViewModel/RecipientMediaViewModel
                    looksLikeUndecryptedGarbage(text) -> {
                        android.util.Log.e("PHOENX_BOOK", "Récit suspect (forme non textuelle) pour ${parent.id}, repli sur le titre")
                        null
                    }
                    else -> text
                }
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_BOOK", "Échec déchiffrement du Récit pour ${parent.id}", e)
                null
            }
            val effectiveSummary = decryptedRecit ?: parent.userTitle.ifBlank { parent.aiSummary }

            // Chantier fusion des compléments (4 septembre) : date de l'ÉVÉNEMENT choisie par le
            // Créateur sur l'écran du souvenir — jamais une date de saisie/modification. Sert à
            // signaler à l'IA que le Récit principal et un complément peuvent parler d'un moment
            // différent. Aucun repli inventé : null si le Créateur n'a rien renseigné. Les
            // compléments n'ont aucune date d'événement propre exploitable (jamais éditable dans
            // l'app pour un complément) — volontairement absente de "stories" ci-dessous.
            val summaryEventDate: String? = (parent.memoryDate ?: parent.memoryDateStart)?.let { millis ->
                eventDateFormatter.format(java.util.Date(millis))
            }

            mapOf(
                "id" to parent.id, // v9.3.1
                "summary" to effectiveSummary,
                "summaryEventDate" to summaryEventDate,
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
        
        // Lot 1 : S'assurer qu'un plan existe systématiquement
        val effectivePlan = plan ?: generateBookPlan()
        
        // v9.1 : Récupération du profil enrichi du Créateur
        val richProfile = offlineEntryDao.getCreatorProfileSync(userId)
        val authorProfileMap = buildAuthorProfileMap(richProfile)

        // v9.3.1 : Calcul du Ton de l'Âme dominant
        val allTones = scenes.mapNotNull { it["soulTone"] as? String }
        val dominantTone = allTones.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        onProgress("Rédaction des chapitres illustrés par l'IA...")
        
        val ageMin = scenes.minOf { it["age"] as Int }
        val ageMax = scenes.maxOf { it["age"] as Int }

        val data = hashMapOf(
            "scenes" to scenes,
            "ageMin" to ageMin,
            "ageMax" to ageMax,
            "soulTone" to dominantTone, // Injection v9.3.1
            "authorProfile" to authorProfileMap, // Transmis à l'IA Biographe v9.1
            "plan" to effectivePlan // Injection v9.3.1 (Lot 1 : plan effectif)
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

        // Lot 3 : rattachement chapitre/sceneIds fiable, indépendant de la position dans le tableau.
        // On privilégie les sceneIds renvoyés par l'IA pour CE chapitre précis ; à défaut (ancienne
        // réponse sans ce champ), on retombe sur le plan par correspondance de TITRE — jamais par index,
        // qui ne garantit rien si l'IA ne restitue pas ses chapitres dans l'ordre du plan reçu.
        val validSceneIds = scenes.mapNotNull { it["id"] as? String }.toSet()
        val ageById = scenes.mapNotNull { scene ->
            val sceneId = scene["id"] as? String
            val sceneAge = scene["age"] as? Int
            if (sceneId != null && sceneAge != null) sceneId to sceneAge else null
        }.toMap()

        data class RawChapterInfo(
            val title: String,
            val content: String,
            val sceneIds: List<String>,
            val minAge: Int
        )

        val rawInfos = rawChapters.mapIndexed { index, ch ->
            val content = ch["content"] as String
            val title = ch["title"] as String

            val aiReportedSceneIds = (ch["sceneIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val fallbackPlanSceneIds = if (aiReportedSceneIds.isEmpty()) {
                (effectivePlan.firstOrNull { it["title"] == title }?.get("sceneIds") as? List<*>)
                    ?.mapNotNull { it?.toString() } ?: emptyList()
            } else emptyList()

            val chapterSceneIds = aiReportedSceneIds.ifEmpty { fallbackPlanSceneIds }
                .filter { it in validSceneIds }

            // Chapitre non résolu (aucun sceneId valide) : relégué en fin de livre, dans son ordre
            // d'arrivée d'origine, plutôt que planté ou mélangé au reste.
            val minAge = chapterSceneIds.mapNotNull { ageById[it] }.minOrNull() ?: (ageMax + 1 + index)

            RawChapterInfo(title, content, chapterSceneIds, minAge)
        }

        // Lot 3 : ordre chronologique imposé par le code, jamais laissé à la seule discrétion de
        // l'IA — son ordre de sortie n'est pas garanti stable d'une régénération à l'autre.
        val orderedInfos = rawInfos.sortedBy { it.minAge }

        // Lot 2 : base pour les empreintes fines par souvenir (tableau de bord de régénération)
        val sceneMapById = scenes.associateBy { it["id"] as? String ?: "" }

        val chapters = orderedInfos.mapIndexed { index, info ->
            val fingerprint = computeChapterFingerprint(
                chapterTitle = info.title,
                chapterSceneIds = info.sceneIds,
                scenes = scenes,
                ageMin = ageMin,
                ageMax = ageMax,
                soulTone = dominantTone,
                authorProfileMap = authorProfileMap
            )

            val sceneDiagnostics = info.sceneIds.mapNotNull { sceneId ->
                sceneMapById[sceneId]?.let { scene -> sceneId to computeSceneDiagnostic(scene) }
            }.toMap()

            BookChapter(
                id = java.util.UUID.randomUUID().toString(),
                title = info.title,
                content = encryptionManager.encryptText(info.content, bookKey).let {
                    android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT)
                },
                status = ChapterStatus.DRAFT,
                orderIndex = index,
                sceneIds = info.sceneIds,
                sourceFingerprint = fingerprint,
                sceneDiagnostics = sceneDiagnostics
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
            sealedMessage = existingDraft?.sealedMessage ?: "",
            metaFingerprint = computeMetaFingerprint(ageMin, ageMax, dominantTone, authorProfileMap)
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
                    "orderIndex" to chapter.orderIndex,
                    "sceneIds" to chapter.sceneIds,
                    "sourceFingerprint" to chapter.sourceFingerprint,
                    "sceneDiagnostics" to chapter.sceneDiagnostics.mapValues { (_, diag) ->
                        mapOf(
                            "contentHash" to diag.contentHash,
                            "characterHashes" to diag.characterHashes,
                            "complementsHash" to diag.complementsHash,
                            "photoCount" to diag.photoCount,
                            "audioCount" to diag.audioCount,
                            "storyCount" to diag.storyCount
                        )
                    }
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
                "coverUploadedAt" to draft.coverUploadedAt,
                "metaFingerprint" to draft.metaFingerprint
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
        // v9.8.16 : Force la lecture serveur (Source.SERVER) pour éviter le cache local obsolète
        val currentDoc = try {
            db.collection("users").document(userId)
                .collection("book").document("current_draft")
                .get(com.google.firebase.firestore.Source.SERVER).await()
        } catch (e: Exception) {
            db.collection("users").document(userId)
                .collection("book").document("current_draft")
                .get().await()
        }
        
        if (currentDoc.exists()) {
            val data = currentDoc.data ?: return
            
            // Log de diagnostic v9.8.16
            val chapters = data["chapters"] as? List<*>
            val firstChapter = chapters?.firstOrNull() as? Map<*, *>
            val firstContent = (firstChapter?.get("content") as? String) ?: ""
            val preview = if (firstContent.length > 30) firstContent.substring(0, 30) else firstContent
            android.util.Log.d("PHOENX_BOOK_BACKUP", "BACKUP: Chapitres=${chapters?.size ?: 0}, Extrait Chiffré Ch1='$preview'")

            db.collection("users").document(userId)
                .collection("book").document("backup_draft")
                .set(data)
                .await()
        }

        val keyDoc = try {
            db.collection("users").document(userId)
                .collection("book_keys").document("main")
                .get(com.google.firebase.firestore.Source.SERVER).await()
        } catch (e: Exception) {
            db.collection("users").document(userId)
                .collection("book_keys").document("main")
                .get().await()
        }

        if (keyDoc.exists()) {
            val keyData = keyDoc.data ?: return
            db.collection("users").document(userId)
                .collection("book_keys").document("backup")
                .set(keyData)
                .await()
        }
        android.util.Log.d("PHOENX_BOOK", "Backup du manuscrit ET de sa clé créés.")
    }

    suspend fun restoreFromBackup(userId: String) {
        val backupDoc = try {
            db.collection("users").document(userId)
                .collection("book").document("backup_draft")
                .get(com.google.firebase.firestore.Source.SERVER).await()
        } catch (e: Exception) {
            db.collection("users").document(userId)
                .collection("book").document("backup_draft")
                .get().await()
        }
        
        if (!backupDoc.exists()) {
            throw Exception("Aucune sauvegarde disponible à restaurer.")
        }

        val data = backupDoc.data ?: return

        // Log de diagnostic v9.8.16
        val chapters = data["chapters"] as? List<*>
        val firstChapter = chapters?.firstOrNull() as? Map<*, *>
        val firstContent = (firstChapter?.get("content") as? String) ?: ""
        val preview = if (firstContent.length > 30) firstContent.substring(0, 30) else firstContent
        android.util.Log.d("PHOENX_BOOK_BACKUP", "RESTORE: Chapitres=${chapters?.size ?: 0}, Extrait Chiffré Ch1='$preview'")

        db.collection("users").document(userId)
            .collection("book").document("current_draft")
            .set(data)
            .await()

        val keyBackupDoc = try {
            db.collection("users").document(userId)
                .collection("book_keys").document("backup")
                .get(com.google.firebase.firestore.Source.SERVER).await()
        } catch (e: Exception) {
            db.collection("users").document(userId)
                .collection("book_keys").document("backup")
                .get().await()
        }

        if (keyBackupDoc.exists()) {
            val keyData = keyBackupDoc.data ?: return
            db.collection("users").document(userId)
                .collection("book_keys").document("main")
                .set(keyData)
                .await()
        }

        // Nettoyage des deux sauvegardes ensemble
        db.collection("users").document(userId)
            .collection("book").document("backup_draft")
            .delete()
            .await()
        db.collection("users").document(userId)
            .collection("book_keys").document("backup")
            .delete()
            .await()

        android.util.Log.d("PHOENX_BOOK", "Restauration du manuscrit ET de sa clé réussie.")
    }

    private fun computeChapterFingerprint(
        chapterTitle: String,
        chapterSceneIds: List<String>,
        scenes: List<Map<String, Any?>>,
        ageMin: Int,
        ageMax: Int,
        soulTone: String?,
        authorProfileMap: Map<String, Any>?
    ): String {
        return try {
            val sortedSceneIds = chapterSceneIds.sorted()
            val sceneMapById = scenes.associateBy { it["id"] as? String ?: "" }
            val chapterScenes = sortedSceneIds.mapNotNull { sceneMapById[it] }

            val fingerprintData = mutableMapOf<String, Any?>(
                "chapterTitle" to chapterTitle,
                "sceneIds" to sortedSceneIds,
                "scenes" to chapterScenes,
                "ageMin" to ageMin,
                "ageMax" to ageMax,
                "soulTone" to soulTone,
                "authorProfile" to authorProfileMap
            )

            val canonicalJsonString = convertToJsonValue(fingerprintData).toString()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(canonicalJsonString.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK", "Échec calcul fingerprint chapitre", e)
            ""
        }
    }

    private fun convertToJsonValue(obj: Any?): Any {
        return when (obj) {
            null -> org.json.JSONObject.NULL
            is Map<*, *> -> {
                val jsonObject = org.json.JSONObject()
                val sortedKeys = obj.keys.mapNotNull { it?.toString() }.sorted()
                for (key in sortedKeys) {
                    jsonObject.put(key, convertToJsonValue(obj[key]))
                }
                jsonObject
            }
            is List<*> -> {
                val jsonArray = org.json.JSONArray()
                for (item in obj) {
                    jsonArray.put(convertToJsonValue(item))
                }
                jsonArray
            }
            is Number, is Boolean, is String -> obj
            else -> obj.toString()
        }
    }

    /**
     * Construit le bloc authorProfile transmis à l'IA à partir du Portrait de Vie.
     * Factorisé (Lot 2) pour être utilisé identiquement par generateBook() et le tableau de bord.
     */
    private fun buildAuthorProfileMap(richProfile: CreatorProfileEntity?): Map<String, Any>? {
        return richProfile?.let { p ->
            val map = mutableMapOf<String, Any>()
            if (!p.bio.isNullOrBlank()) map["bio"] = p.bio
            if (!p.profession.isNullOrBlank()) map["profession"] = p.profession
            if (p.hasSiblings == true && !p.siblingsDetail.isNullOrBlank()) map["family_siblings"] = p.siblingsDetail
            if (p.hasChildren == true && !p.childrenDetail.isNullOrBlank()) map["family_children"] = p.childrenDetail
            if (!p.hobbies.isNullOrBlank()) map["hobbies"] = p.hobbies
            val physical = mutableListOf<String>()
            p.height?.let { physical.add("$it cm") }
            p.weight?.let { physical.add("$it kg") }
            p.eyeColor?.let { physical.add("Yeux $it") }
            p.hairColor?.let { physical.add("Cheveux $it") }
            if (physical.isNotEmpty()) map["physical_appearance"] = physical.joinToString(", ")

            if (map.isNotEmpty()) map else null
        }
    }

    /**
     * Étape 3 : dernier recours si le déchiffrement du Récit ne lève aucune exception et ne renvoie
     * pas le sentinel "Contenu chiffré", mais produit tout de même du charabia non textuel (clé
     * périmée après une restauration bancale, par ex.). Un vrai Récit en français contient des
     * espaces ; un blob chiffré/base64 mal déchiffré n'en contient jamais sur une telle longueur.
     */
    private fun looksLikeUndecryptedGarbage(text: String): Boolean {
        if (text.length < 20) return false
        if (!text.contains(' ')) return true
        val base64ish = text.count { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
        return base64ish.toDouble() / text.length > 0.98
    }

    private fun sha256OfJson(data: Any?): String {
        return try {
            val canonicalJsonString = convertToJsonValue(data).toString()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            digest.digest(canonicalJsonString.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Lot 2 : empreinte fine d'UN souvenir (scène au format extractScenes()), décomposée par nature
     * de contenu pour permettre une raison en clair précise dans le tableau de bord de régénération.
     */
    private fun computeSceneDiagnostic(scene: Map<String, Any?>): SceneDiagnostic {
        val contentHash = sha256OfJson(
            mapOf(
                "summary" to scene["summary"],
                "summaryEventDate" to scene["summaryEventDate"], // chantier fusion des compléments (4 septembre)
                "userComment" to scene["userComment"],
                "amendments" to scene["amendments"],
                "category" to scene["category"],
                "tonalNuance" to scene["tonalNuance"],
                "soulTone" to scene["soulTone"],
                "originType" to scene["originType"]
            )
        )

        @Suppress("UNCHECKED_CAST")
        val characters = scene["characters"] as? List<Map<String, Any?>> ?: emptyList()
        val characterHashes = characters.associate { c ->
            val name = "${c["firstName"] ?: ""} ${c["lastName"] ?: ""}".trim().ifBlank { "?" }
            name to sha256OfJson(c)
        }

        @Suppress("UNCHECKED_CAST")
        val photos = scene["photos"] as? List<Map<String, Any?>> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val vocals = scene["vocal_essence"] as? List<Map<String, Any?>> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val stories = scene["stories"] as? List<Map<String, Any?>> ?: emptyList()

        val complementsHash = sha256OfJson(mapOf("photos" to photos, "vocal_essence" to vocals, "stories" to stories))

        return SceneDiagnostic(
            contentHash = contentHash,
            characterHashes = characterHashes,
            complementsHash = complementsHash,
            photoCount = photos.size,
            audioCount = vocals.size,
            storyCount = stories.size
        )
    }

    private fun computeMetaFingerprint(ageMin: Int, ageMax: Int, soulTone: String?, authorProfileMap: Map<String, Any>?): String {
        return sha256OfJson(mapOf("ageMin" to ageMin, "ageMax" to ageMax, "soulTone" to soulTone, "authorProfile" to authorProfileMap))
    }

    private fun truncateLabel(text: String?, max: Int = 40): String {
        val t = text?.trim().orEmpty().ifBlank { return "ce souvenir" }
        return if (t.length <= max) t else t.take(max).trimEnd() + "…"
    }

    /**
     * Lot 2 : Tableau de bord de régénération. Comparaison 100% locale, aucun appel IA — recalcule
     * les empreintes à partir du contenu réel actuel et les compare à celles stockées à la dernière
     * génération. Ne modifie rien, ne déclenche aucune régénération.
     */
    suspend fun computeRegenerationDashboard(userId: String): RegenerationDashboard {
        val draft = loadBookDraft(userId) ?: return RegenerationDashboard()
        val scenes = extractScenes()
        val sceneMapById = scenes.associateBy { it["id"] as? String ?: "" }

        val richProfile = offlineEntryDao.getCreatorProfileSync(userId)
        val authorProfileMap = buildAuthorProfileMap(richProfile)
        val allTones = scenes.mapNotNull { it["soulTone"] as? String }
        val dominantTone = allTones.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val ageMin = scenes.mapNotNull { it["age"] as? Int }.minOrNull() ?: 0
        val ageMax = scenes.mapNotNull { it["age"] as? Int }.maxOrNull() ?: 0

        val currentMetaFingerprint = computeMetaFingerprint(ageMin, ageMax, dominantTone, authorProfileMap)
        val globalReason = if (draft.metaFingerprint.isNotEmpty() && draft.metaFingerprint != currentMetaFingerprint) {
            "Votre Portrait de Vie ou le Ton de l'Âme global du livre a changé depuis la dernière génération — cela concerne tous les chapitres."
        } else null

        fun mediaReason(label: String, storedCount: Int, currentCount: Int, sceneLabel: String): String? = when {
            currentCount > storedCount -> "Vous avez ajouté $label à « $sceneLabel »."
            currentCount < storedCount -> "Vous avez retiré $label de « $sceneLabel »."
            else -> null
        }

        val chapterInfos = draft.chapters.sortedBy { it.orderIndex }.map { chapter ->
            val reasons = mutableListOf<String>()

            if (chapter.sceneDiagnostics.isEmpty() && chapter.sceneIds.isNotEmpty()) {
                // Repli (chapitre généré avant le Lot 2) : pas de détail fin disponible, on retombe
                // sur le hash global déjà connu (Lot 1/3).
                val fallbackFingerprint = computeChapterFingerprint(
                    chapterTitle = chapter.title,
                    chapterSceneIds = chapter.sceneIds,
                    scenes = scenes,
                    ageMin = ageMin,
                    ageMax = ageMax,
                    soulTone = dominantTone,
                    authorProfileMap = authorProfileMap
                )
                if (fallbackFingerprint != chapter.sourceFingerprint) {
                    reasons.add("Le contenu source a changé depuis la dernière génération — détail précis disponible après la prochaine régénération complète.")
                }
            } else {
                for (sceneId in chapter.sceneIds) {
                    val storedDiag = chapter.sceneDiagnostics[sceneId]
                    val currentScene = sceneMapById[sceneId]
                    if (currentScene == null) {
                        reasons.add("Un souvenir de ce chapitre n'est plus disponible ou a été retiré du Livre.")
                        continue
                    }
                    if (storedDiag == null) continue // sceneId ajouté hors génération (ne devrait pas arriver), ignoré

                    val currentDiag = computeSceneDiagnostic(currentScene)
                    val sceneLabel = truncateLabel(currentScene["summary"] as? String)

                    if (currentDiag.contentHash != storedDiag.contentHash) {
                        reasons.add("Vous avez modifié le souvenir « $sceneLabel ».")
                    }

                    val allNames = storedDiag.characterHashes.keys + currentDiag.characterHashes.keys
                    for (name in allNames) {
                        if (storedDiag.characterHashes[name] != currentDiag.characterHashes[name]) {
                            reasons.add("Vous avez enrichi la fiche de $name dans l'Arbre.")
                        }
                    }

                    mediaReason("une photo", storedDiag.photoCount, currentDiag.photoCount, sceneLabel)?.let { reasons.add(it) }
                    mediaReason("un enregistrement audio", storedDiag.audioCount, currentDiag.audioCount, sceneLabel)?.let { reasons.add(it) }
                    mediaReason("un récit", storedDiag.storyCount, currentDiag.storyCount, sceneLabel)?.let { reasons.add(it) }

                    val countsUnchanged = currentDiag.photoCount == storedDiag.photoCount &&
                        currentDiag.audioCount == storedDiag.audioCount &&
                        currentDiag.storyCount == storedDiag.storyCount
                    if (countsUnchanged && currentDiag.complementsHash != storedDiag.complementsHash) {
                        reasons.add("Vous avez modifié la description d'un média de « $sceneLabel ».")
                    }
                }
            }

            ChapterRegenInfo(
                chapterId = chapter.id,
                title = chapter.title,
                orderIndex = chapter.orderIndex,
                status = if (reasons.isNotEmpty() || globalReason != null) ChapterRegenStatus.TO_REWORK else ChapterRegenStatus.INTACT,
                reasons = reasons.distinct()
            )
        }

        val allChapterSceneIds = draft.chapters.flatMap { it.sceneIds }.toSet()
        val orphanScenes = scenes.filter { (it["id"] as? String ?: "") !in allChapterSceneIds }
            .map { scene ->
                OrphanSceneInfo(
                    sceneId = scene["id"] as? String ?: "",
                    summary = truncateLabel(scene["summary"] as? String, 60),
                    age = scene["age"] as? Int ?: 0
                )
            }

        return RegenerationDashboard(
            chapters = chapterInfos,
            orphanScenes = orphanScenes,
            globalReason = globalReason
        )
    }
}
