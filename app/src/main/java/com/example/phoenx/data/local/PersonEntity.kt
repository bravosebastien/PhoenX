package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String? = null,
    val relationship: String? = null, // ex: "compagne", "cousin"
    val distinctionType: String? = null, // "nom_famille", "surnom", "ville", "autre"
    val distinctionValue: String? = null,
    val imagePath: String? = null, // v8.9.9 : Chemin vers le portrait Cameo
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending",

    // ÉCRAN PERSONNAGES v9.0
    val height: Int? = null,
    val weight: Int? = null,
    val eyeColor: String? = null,
    val hairColor: String? = null,
    val clothingStyle: String? = null,
    val profession: String? = null,
    val hasChildren: Boolean? = null,
    val relationshipDetail: String? = null
)
