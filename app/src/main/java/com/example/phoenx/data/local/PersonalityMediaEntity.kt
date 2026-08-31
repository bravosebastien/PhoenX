package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "personality_media",
    foreignKeys = [
        ForeignKey(
            entity = PersonalityEntity::class,
            parentColumns = ["id"],
            childColumns = ["personalityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["personalityId"])]
)
data class PersonalityMediaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val personalityId: String,
    val mediaPath: String, // Chemin local ou URL Storage
    val capturedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)
