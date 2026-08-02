package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "person_media",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["personId"])]
)
data class PersonMediaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val personId: String,
    val mediaPath: String, // Chemin local ou URL Storage
    val mediaType: String, // "PHOTO" | "VIDEO"
    val capturedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)
