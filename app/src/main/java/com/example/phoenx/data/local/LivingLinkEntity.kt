package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "living_links")
data class LivingLinkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val creatorId: String,
    val recipientId: String,
    val recipientName: String,
    val type: String, // "TEXT", "PHOTO", "VIDEO", "AUDIO"
    val status: String = "pending", // "pending", "sent"
    val scheduledAt: Long? = null,
    val sentAt: Long? = null,
    val originalEntryId: String? = null,
    val syncStatus: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)
