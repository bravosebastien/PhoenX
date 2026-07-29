package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "standalone_media",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["type"])
    ]
)
data class StandaloneMediaEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val creatorUid: String = "",
    val type: String, // "TEXT_EXCERPT", "SPOTIFY", "YOUTUBE", "PHOTO"
    val title: String = "",
    val description: String? = null, // v9.3.3
    val content: String, // Texte chiffré (Base64) ou URL en clair
    val recipientIds: String = "", // CSV des vrais UIDs
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending" // "pending" | "synced" | "failed"
)
