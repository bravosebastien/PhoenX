package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recipients")
data class RecipientEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val relationship: String,
    val accessLevel: String = "full", // "partial" | "full"
    val canAskQuestions: Boolean = false,
    val maxQuestionsAllowed: Int? = null,
    val questionsAskedCount: Int = 0,
    val invitationSentAt: Long? = null,
    val invitationConfirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
