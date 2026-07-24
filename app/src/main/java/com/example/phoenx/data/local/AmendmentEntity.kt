package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "amendments",
    indices = [Index(value = ["entryId"])]
)
data class AmendmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entryId: String,
    val encryptedContent: ByteArray,
    val ageAtAmendment: String, // JSON
    val createdAt: Long = System.currentTimeMillis(),
    val aiEvolution: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AmendmentEntity

        if (id != other.id) return false
        if (entryId != other.entryId) return false
        if (!encryptedContent.contentEquals(other.encryptedContent)) return false
        if (ageAtAmendment != other.ageAtAmendment) return false
        if (createdAt != other.createdAt) return false
        if (aiEvolution != other.aiEvolution) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + entryId.hashCode()
        result = 31 * result + encryptedContent.contentHashCode()
        result = 31 * result + ageAtAmendment.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (aiEvolution?.hashCode() ?: 0)
        return result
    }
}
