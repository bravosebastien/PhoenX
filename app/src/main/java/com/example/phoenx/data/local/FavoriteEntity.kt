package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String, // "book" | "film" | "music" | "travel"
    val encryptedTitle: ByteArray,
    val encryptedWhy: ByteArray,
    val period: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
