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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PortraitEntity

        if (id != other.id) return false
        if (recipientId != other.recipientId) return false
        if (!encryptedContent.contentEquals(other.encryptedContent)) return false
        if (isTransmitted != other.isTransmitted) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + recipientId.hashCode()
        result = 31 * result + encryptedContent.contentHashCode()
        result = 31 * result + isTransmitted.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
