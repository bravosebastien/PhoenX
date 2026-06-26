package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "legacies")
data class LegacyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipientId: String,
    val entryIds: String, // IDs séparés par des virgules
    val triggerType: String, // "date" | "activation" | "unique_key"
    val triggerTimestamp: Long? = null,
    val isUniqueKey: Boolean = false,
    val uniqueKeyHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
