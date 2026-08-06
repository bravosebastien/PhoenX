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
    val userComment: String? = null, // v9.4.27 (anciennement description)
    val content: String, // Texte chiffré (Base64) ou URL en clair
    val recipientIds: String = "", // CSV des vrais UIDs
    val visibility: String = "RESTRICTED", // v9.4.19
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending", // "pending" | "synced" | "failed"

    // ENRICHISSEMENT MÉDIA v9.4.27 (Migration v42)
    val coverUrl: String? = null,
    val localCoverPath: String? = null,
    val mediaProvider: String? = null // "SPOTIFY", "DEEZER", "YOUTUBE"
)
