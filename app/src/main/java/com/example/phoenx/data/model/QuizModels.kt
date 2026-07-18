package com.example.phoenx.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Quiz(
    @DocumentId val id: String = "",
    val title: String = "",
    val isActive: Boolean = true,
    val availableAfterDeath: Boolean = true,
    val availableNow: Boolean = false,
    val showNames: Boolean = true,
    val recipientIds: List<String> = emptyList(), // v8.3
    val questions: List<QuizQuestion> = emptyList(),
    val finalMessage: String = ""
)

data class QuizQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "PHOTO", "AUDIO", "VIDEO"
    val correctAnswer: String = "", // Chiffré Tink (v8.3)
    val correctHash: String = "", // Pour validation Hard mode
    val distractors: List<String> = emptyList(),
    val teasingMessages: List<String> = emptyList(),
    val recipientIds: List<String> = emptyList(),
    val difficultyAllowed: Boolean = true
)

data class QuizResult(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val recipientName: String? = null, // Rempli si Quiz.showNames = true
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val completedAt: Timestamp = Timestamp.now(),
    val answers: List<String> = emptyList(), // Réponses données
    val helpUsed: Boolean = false // Flag pour message final (v8.3)
)
