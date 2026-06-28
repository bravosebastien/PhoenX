package com.example.phoenx.data.model

import java.util.UUID

data class BookChapter(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val status: ChapterStatus = ChapterStatus.DRAFT,
    val lastModified: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
)

enum class ChapterStatus {
    DRAFT,
    IN_REVIEW,
    VALIDATED
}

data class BookDraft(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val status: BookStatus = BookStatus.DRAFT,
    val chapters: List<BookChapter> = emptyList(),
    val totalEntries: Int = 0
)

enum class BookStatus {
    DRAFT,
    IN_PROGRESS,
    COMPLETE
}

data class BookMetadata(
    val summaries: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val ages: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap()
)

data class AiMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
