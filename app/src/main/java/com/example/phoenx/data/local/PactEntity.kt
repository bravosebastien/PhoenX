package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pacts")
data class PactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val partnerName: String,
    val partnerEmail: String,
    val status: String = "pending", // "pending" | "active"
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PactEntity
        return id == other.id && partnerName == other.partnerName && partnerEmail == other.partnerEmail && status == other.status && createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + partnerName.hashCode()
        result = 31 * result + partnerEmail.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
