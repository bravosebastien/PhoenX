package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "depositaries")
data class DepositaryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val phone: String,
    val status: String = "invited", // "invited" | "confirmed" | "active"
    val createdAt: Long = System.currentTimeMillis()
)
