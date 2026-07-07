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
    val questions: List<QuizQuestion> = emptyList(),
    val finalMessage: String = ""
)

data class QuizQuestion(
    val id: String = "",
    val question: String = "",
    val answers: List<String> = emptyList(), // Max 4
    val correctIndex: Int = 0,
    val points: Int = 1
)

data class QuizResult(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val recipientName: String? = null, // Rempli si Quiz.showNames = true
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val completedAt: Timestamp = Timestamp.now(),
    val answers: List<Int> = emptyList() // Index choisi pour chaque question
)
