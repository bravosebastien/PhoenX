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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FavoriteEntity

        if (id != other.id) return false
        if (category != other.category) return false
        if (!encryptedTitle.contentEquals(other.encryptedTitle)) return false
        if (!encryptedWhy.contentEquals(other.encryptedWhy)) return false
        if (period != other.period) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + encryptedTitle.contentHashCode()
        result = 31 * result + encryptedWhy.contentHashCode()
        result = 31 * result + period.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
