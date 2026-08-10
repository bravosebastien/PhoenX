package com.example.phoenx.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

enum class ChapterStatus { DRAFT, IN_REVIEW, VALIDATED }
enum class BookStatus { DRAFT, IN_PROGRESS, COMPLETE }

data class BookChapter(
    val id: String = "",
    val title: String = "",
    val content: String = "", // Chiffré Tink
    val status: ChapterStatus = ChapterStatus.DRAFT,
    val lastModified: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
)

data class BookTheme(
    val backgroundId: String = "classic_ivory",
    val fontId: String = "eb_garamond"
)

data class BookDraft(
    val id: String = "",
    val userId: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val status: BookStatus = BookStatus.DRAFT,
    val chapters: List<BookChapter> = emptyList(),
    val totalEntries: Int = 0,
    val bookTitle: String? = null, // v9.2: Titre personnalisé du Livre
    val recipientIds: List<String> = emptyList(), // v8.5.4 Parity of access
    val sealedMessage: String = "", // v8.6.2 Message personnalisé pour l'héritier
    val globalIntroduction: String = "", // v8.7.0 Intro globale du livre (Chiffrée)
    val theme: BookTheme = BookTheme(), // v8.7.0 Thème visuel choisi par le Créateur
    val coverImageUrl: String? = null, // v9.2.4: Image de couverture personnalisée
    val coverTitleStyle: String = "GOLD", // v9.2.7: Style du titre par défaut
    val visibility: String = "RESTRICTED" // v9.4.27: Sécurisation explicite
)

data class BookMetadata(
    val summaries: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val ages: List<Int> = emptyList(),
    val categories: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap()
)

data class AiMessage(
    val role: String = "user", // "user" | "model"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
