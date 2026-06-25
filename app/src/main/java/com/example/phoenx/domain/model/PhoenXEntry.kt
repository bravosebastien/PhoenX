package com.example.phoenx.domain.model

import java.time.Instant

/**
 * Modèle de base pour une entrée dans le Fil de Pensée.
 */
data class PhoenXEntry(
    val id: String = "",
    val creatorUid: String = "",
    val timestamp: Instant = Instant.now(),
    val ageAtCreation: AgeSnapshot,
    val encryptedContent: ByteArray,
    val type: EntryType = EntryType.THOUGHT,
    val tags: List<String> = emptyList(),
    val isYoungSelfLetter: Boolean = false,
    val targetAge: Int? = null,
    val amendments: List<PhoenXAmendment> = emptyList(),
    
    // IA SIGNATURE 5.0
    val aiSummary: String = "",
    val aiTags: List<String> = emptyList()
)

data class PhoenXAmendment(
    val id: String,
    val encryptedContent: ByteArray,
    val ageAtAmendment: AgeSnapshot,
    val createdAt: Instant
)

enum class EntryType {
    THOUGHT,
    EMOTION,
    LEGACY,
    NIGHT_CAPTURE
}
