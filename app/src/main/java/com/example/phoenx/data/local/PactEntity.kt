package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pacts")
data class PactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val partnerId: String? = null,
    val partnerName: String,
    val partnerEmail: String,
    val status: String = "pending", // "pending" | "active" | "completed"
    val myStatus: String = "writing", // "writing" | "completed"
    val partnerStatus: String = "writing", // "writing" | "completed"
    val myConsentToBook: Boolean = false,
    val partnerConsentToBook: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
