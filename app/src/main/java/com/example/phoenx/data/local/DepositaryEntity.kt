package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "depositaries")
data class DepositaryEntity(
    @PrimaryKey val id: String, // "primary" ou "secondary"
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String? = null, // v9.1 : "primary" | "secondary"
    val status: String = "invited", // "invited" | "active"
    val photoUrl: String? = null, // v9.2.2: Profile photo
    val linkedUid: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
