package com.example.phoenx.data.model

import java.util.UUID

data class StandaloneMedia(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String, // "TEXT_EXCERPT", "SPOTIFY", "YOUTUBE", "PHOTO"
    val title: String = "",
    val userComment: String? = null, // v9.4.27
    val content: String, // Texte chiffré ou URL en clair
    val recipientIds: List<String> = emptyList(), // Vrais UIDs
    val createdAt: Long = System.currentTimeMillis()
)
