package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "portraits")
data class PortraitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipientId: String,
    val encryptedContent: ByteArray,
    val isTransmitted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
