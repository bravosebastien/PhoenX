package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "personalities")
data class PersonalityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String, // Sport, Cinéma, Peinture, Sculpture, Sciences, Symboles de la paix, Symboles du chaos, Symboles de l'amour, Symboles de la haine, Autre
    val customCategoryLabel: String? = null,
    val mainPhotoPath: String, // Chemin local ou URL Storage
    val biography: String = "",
    val personalComment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)
