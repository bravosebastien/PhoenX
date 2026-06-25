package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "offline_entries")
data class OfflineEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val encryptedPayload: ByteArray,
    val entryType: String,
    val ageAtCreation: String, // Stocké en JSON
    val emotionalCategory: String,
    val visibility: String,
    val isYoungSelfLetter: Boolean = false,
    val targetAge: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending", // "pending" | "synced" | "failed"
    
    // CHAMPS IA LOCALE (Signature 5.0)
    val aiSummary: String = "",
    val aiTags: String = "", // Tags séparés par des virgules pour Room

    // MODE DÉTECTIVE & BOÎTE AUX LETTRES (ADN 5.0)
    val enigmaQuestion: String? = null,
    val enigmaAnswer: String? = null,
    val scheduledTimestamp: Long? = null
)
