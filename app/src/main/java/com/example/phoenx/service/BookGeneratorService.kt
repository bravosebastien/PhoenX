package com.example.phoenx.service

import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookGeneratorService @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val encryptionManager: EncryptionManager
) {

    // ── EXTRACTEUR ─────────────────────────────
    suspend fun extractMetadata(userId: String): BookMetadata {
        val entriesSnap = db.collection("users")
            .document(userId)
            .collection("entries")
            .get()
            .await()

        val locationsSnap = db.collection("users")
            .document(userId)
            .collection("locations")
            .get()
            .await()

        val summaries = entriesSnap.documents
            .mapNotNull { it.getString("aiSummary") }
            .filter { it.isNotBlank() }

        val tags = entriesSnap.documents
            .flatMap { doc ->
                @Suppress("UNCHECKED_CAST")
                (doc.get("aiTags") as? List<String>) ?: emptyList()
            }
            .distinct()

        val ages = entriesSnap.documents
            .mapNotNull { it.getString("ageAtCreation.displayLabel") }

        val categories = entriesSnap.documents
            .mapNotNull { it.getString("emotionalCategory") }

        val categoryCounts = categories
            .groupingBy { it }
            .eachCount()

        val places = locationsSnap.documents
            .mapNotNull { it.getString("placeName") }
            .distinct()

        return BookMetadata(
            summaries = summaries,
            tags = tags,
            ages = ages,
            categories = categories,
            places = places,
            categoryCounts = categoryCounts
        )
    }

    // ── COMPILATEUR ────────────────────────────
    suspend fun generateBook(
        userId: String,
        onProgress: (String) -> Unit
    ): BookDraft {

        onProgress("Lecture de tes souvenirs...")
        val metadata = extractMetadata(userId)

        if (metadata.summaries.isEmpty()) {
            throw Exception("Pas assez de souvenirs pour générer un livre.")
        }

        onProgress("Analyse des thèmes de ta vie...")
        delay(800)

        onProgress("Rédaction du plan de ton livre...")

        // Appel Cloud Function
        val data = hashMapOf(
            "summaries" to metadata.summaries.take(50),
            "tags" to metadata.tags.take(30),
            "categoryCounts" to metadata.categoryCounts,
            "places" to metadata.places.take(20),
            "ageMin" to (metadata.ages.minOrNull() ?: ""),
            "ageMax" to (metadata.ages.maxOrNull() ?: ""),
            "totalEntries" to metadata.summaries.size
        )

        onProgress("L'IA rédige ton histoire...")

        val result = functions
            .getHttpsCallable("generateBookChapters")
            .call(data)
            .await()

        onProgress("Finalisation des chapitres...")

        @Suppress("UNCHECKED_CAST")
        val rawChapters = (result.data as? Map<*, *>)
            ?.get("chapters") as? List<Map<String, Any>>
            ?: throw Exception("Réponse invalide de l'IA.")

        // Chiffrement de chaque chapitre avec Tink
        val chapters = rawChapters.mapIndexed { index, raw ->
            val content = raw["content"] as? String ?: ""
            BookChapter(
                title = raw["title"] as? String ?: "Chapitre ${index + 1}",
                content = encryptionManager.encrypt(content),
                orderIndex = (raw["orderIndex"] as? Long)?.toInt() ?: index,
                status = ChapterStatus.DRAFT
            )
        }

        onProgress("Sauvegarde en cours...")

        val draft = BookDraft(
            userId = userId,
            chapters = chapters,
            totalEntries = metadata.summaries.size,
            status = BookStatus.DRAFT
        )

        saveBookDraft(userId, draft)
        return draft
    }

    // ── SAUVEGARDE ─────────────────────────────
    suspend fun saveBookDraft(userId: String, draft: BookDraft) {
        val chaptersData = draft.chapters.map { chapter ->
            mapOf(
                "id" to chapter.id,
                "title" to chapter.title,
                "content" to chapter.content,
                "status" to chapter.status.name,
                "lastModified" to FieldValue.serverTimestamp(),
                "orderIndex" to chapter.orderIndex
            )
        }
        db.collection("users")
            .document(userId)
            .collection("book")
            .document(draft.id)
            .set(mapOf(
                "generatedAt" to FieldValue.serverTimestamp(),
                "lastUpdatedAt" to FieldValue.serverTimestamp(),
                "status" to draft.status.name,
                "totalEntries" to draft.totalEntries,
                "chapters" to chaptersData
            ))
            .await()
    }

    // ── CHARGEMENT ─────────────────────────────
    suspend fun loadBookDraft(userId: String): BookDraft? {
        val snap = db.collection("users")
            .document(userId)
            .collection("book")
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        if (snap.isEmpty) return null
        val doc = snap.documents.first()
        @Suppress("UNCHECKED_CAST")
        val rawChapters = doc.get("chapters") as? List<Map<String, Any>>
            ?: return null
        val chapters = rawChapters.map { raw ->
            BookChapter(
                id = raw["id"] as? String ?: "",
                title = raw["title"] as? String ?: "",
                content = encryptionManager.decrypt(
                    raw["content"] as? String ?: ""
                ),
                status = ChapterStatus.valueOf(
                    raw["status"] as? String ?: "DRAFT"
                ),
                orderIndex = (raw["orderIndex"] as? Long)?.toInt() ?: 0
            )
        }.sortedBy { it.orderIndex }
        return BookDraft(
            id = doc.id,
            userId = userId,
            chapters = chapters,
            totalEntries = (doc.getLong("totalEntries") ?: 0).toInt(),
            status = BookStatus.valueOf(
                doc.getString("status") ?: "DRAFT"
            )
        )
    }

    // ── MODIFICATION PAR L'IA ──────────────────
    suspend fun askAiToModifyChapter(
        currentContent: String,
        instruction: String
    ): String {
        val data = hashMapOf(
            "currentContent" to currentContent,
            "instruction" to instruction
        )
        val result = functions
            .getHttpsCallable("modifyBookChapter")
            .call(data)
            .await()
        return (result.data as? Map<*, *>)
            ?.get("newContent") as? String
            ?: currentContent
    }
}
