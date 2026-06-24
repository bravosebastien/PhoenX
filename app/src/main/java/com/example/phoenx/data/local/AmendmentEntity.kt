package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "amendments")
data class AmendmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entryId: String,
    val encryptedContent: ByteArray,
    val ageAtAmendment: String, // JSON
    val createdAt: Long = System.currentTimeMillis()
)
