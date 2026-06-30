package com.example.phoenx.domain.model

import com.google.firebase.Timestamp

data class PendingQuestion(
    val id: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val questionText: String = "", // Chiffré Tink
    val askedAt: Long = System.currentTimeMillis(),
    val status: String = "pending", // "pending" | "answered" | "declined"
    val answerContent: String? = null, // Chiffré Tink
    val answerType: String? = null, // "text" | "audio" | "video"
    val declineNote: String? = null, // Chiffré Tink
    val answeredAt: Long? = null,
    val linkedEntryId: String? = null
)
