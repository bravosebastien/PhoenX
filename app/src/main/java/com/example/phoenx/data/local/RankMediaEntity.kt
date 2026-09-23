package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "rank_media")
data class RankMediaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rankingId: String,
    val rankIndex: Int,
    val mediaPath: String,
    val mediaType: String = "PHOTO", // "PHOTO" | "VIDEO"
    val thumbnailPath: String? = null,
    val localThumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)
