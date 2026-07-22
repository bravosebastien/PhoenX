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
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)
