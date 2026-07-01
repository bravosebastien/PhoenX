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

    /**
     * Extrait les métadonnées IA de tous les souvenirs locaux.
     */
    suspend fun extractMetadata(): BookMetadata {
        val entries = offlineEntryDao.getAllEntriesSync()
        
        val summaries = entries.map { it.aiSummary }.filter { it.isNotEmpty() }
        val tags = entries.flatMap { it.aiTags.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }
        val ages = entries.map { AgeUtils.parseAgeJson(it.ageAtCreation).years }
        val categories = entries.map { it.emotionalCategory }
        
        // Extraction des lieux mentionnés dans les métadonnées
        val places = entries.map { it.aiSummary }
            .filter { it.contains("à ") || it.contains("en ") } 
            // Amélioration future : utiliser un détecteur d'entités nommées

        val categoryCounts = categories.groupingBy { it }.eachCount()

        return BookMetadata(
            summaries = summaries,
            tags = tags.distinct(),
            ages = ages,
            categories = categories.distinct(),
            places = places,
            categoryCounts = categoryCounts
        )
    }

    /**
     * Lance la génération complète du manuscrit.
     * @param onProgress Callback pour mettre à jour l'UI pendant les étapes.
     */
    suspend fun generateBook(userId: String, onProgress: (String) -> Unit): BookDraft {
        onProgress("Analyse de tes souvenirs...")
        val metadata = extractMetadata()
        
        if (metadata.summaries.isEmpty()) {
            throw Exception("Pas assez de souvenirs pour écrire ton livre.")
        }

        onProgress("Rédaction des chapitres par l'IA...")
        
        val data = hashMapOf(
            "summaries" to metadata.summaries,
            "tags" to metadata.tags,
            "categoryCounts" to metadata.categoryCounts,
            "ageMin" to (metadata.ages.minOrNull() ?: 0),
            "ageMax" to (metadata.ages.maxOrNull() ?: 0),
            "totalEntries" to metadata.summaries.size,
            "places" to metadata.places
        )

        val result = functions.getHttpsCallable("generateBookChapters")
            .call(data)
            .await()

        val response = result.data as Map<*, *>
        val rawChapters = response["chapters"] as List<Map<*, *>>

        onProgress("Chiffrement et sécurisation...")

        val chapters = rawChapters.map { ch ->
            val content = ch["content"] as String
            BookChapter(
                id = java.util.UUID.randomUUID().toString(),
                title = ch["title"] as String,
                content = encryptionManager.encrypt(content), // Chiffrage Tink simulé ou réel
                status = ChapterStatus.DRAFT,
                orderIndex = (ch["orderIndex"] as Number).toInt()
            )
        }

        val draft = BookDraft(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            chapters = chapters,
            totalEntries = metadata.summaries.size
        )

        onProgress("Sauvegarde finale...")
        saveBookDraft(userId, draft)

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
}
