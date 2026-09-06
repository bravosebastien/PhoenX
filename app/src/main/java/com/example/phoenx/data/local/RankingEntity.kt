package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "rankings")
data class RankingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val itemCount: Int,
    val items: String, // Stocké en CSV ou JSON (on utilisera CSV simple pour items)
    val coverImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)
