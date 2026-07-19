package com.example.phoenx.data.model

import com.google.firebase.Timestamp

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

data class BookDraft(
    val id: String = "",
    val userId: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val status: BookStatus = BookStatus.DRAFT,
    val chapters: List<BookChapter> = emptyList(),
    val totalEntries: Int = 0,
    val recipientIds: List<String> = emptyList() // v8.5.4 Parity of access
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
