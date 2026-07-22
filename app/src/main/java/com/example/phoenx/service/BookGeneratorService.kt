package com.example.phoenx.service

import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.model.BookChapter
import com.example.phoenx.data.model.BookDraft
import com.example.phoenx.data.model.BookMetadata
import com.example.phoenx.data.model.ChapterStatus
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
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
        return try {
            val doc = db.collection("users").document(userId)
                .collection("book").document("current_draft").get().await()
            
            if (doc.exists()) {
                doc.toObject(BookDraft::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK", "Erreur lors du chargement du livre: ${e.message}")
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
        val parents = allEntries.filter { it.parentEntryId == null }
        
        return parents.map { parent ->
            val complements = allEntries.filter { it.parentEntryId == parent.id }
            val age = AgeUtils.parseAgeJson(parent.ageAtCreation)
            
            mapOf(
                "summary" to parent.aiSummary,
                "age" to age.years,
                "category" to parent.emotionalCategory,
                "photos" to complements.filter { it.entryType == "PHOTO" || it.entryType == "GALLERY" }
                    .map { mapOf("id" to it.id, "description" to it.aiSummary) },
                "vocal_essence" to complements.filter { it.entryType == "AUDIO" }
                    .map { mapOf("id" to it.id, "description" to it.aiSummary) },
                "stories" to complements.filter { it.entryType == "TEXT" || it.entryType == "THOUGHT" }
                    .map { mapOf("id" to it.id, "description" to it.aiSummary) }
            )
        }.sortedBy { it["age"] as Int }
    }

    /**
     * Lance la génération complète du manuscrit multimédia.
     */
    suspend fun generateBook(userId: String, onProgress: (String) -> Unit): BookDraft {
        onProgress("Préparation des scènes de ta vie...")
        val scenes = extractScenes()
        
        if (scenes.isEmpty()) {
            throw Exception("Pas assez de souvenirs pour écrire ton livre.")
        }

        onProgress("Rédaction des chapitres illustrés par l'IA...")
        
        val ageMin = scenes.minOf { it["age"] as Int }
        val ageMax = scenes.maxOf { it["age"] as Int }

        val data = hashMapOf(
            "scenes" to scenes,
            "ageMin" to ageMin,
            "ageMax" to ageMax
        )

        val result = functions.getHttpsCallable("generateBookChapters")
            .call(data)
            .await()

        val response = result.data as Map<*, *>
        val rawChapters = response["chapters"] as List<Map<*, *>>

        onProgress("Chiffrement et sécurisation...")

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

        val draft = BookDraft(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            chapters = chapters,
            totalEntries = scenes.size
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

    /**
     * Enregistre le brouillon dans Firestore.
     */
    suspend fun saveBookDraft(userId: String, draft: BookDraft) {
        try {
            db.collection("users").document(userId)
                .collection("book").document("current_draft")
                .set(draft)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK", "Erreur lors de la sauvegarde: ${e.message}")
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
}
