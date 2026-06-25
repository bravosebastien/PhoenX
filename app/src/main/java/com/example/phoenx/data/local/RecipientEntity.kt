package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recipients")
data class RecipientEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val relationship: String,
    val accessLevel: String = "full", // "partial" | "full"
    val createdAt: Long = System.currentTimeMillis()
)
