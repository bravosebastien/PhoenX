package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "witnesses")
data class WitnessEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val status: String = "invited", // "invited" | "submitted" | "validated" | "rejected"
    val submittedAt: Long? = null,
    val allowCreatorToRead: Boolean = false,
    val allowCreatorToReject: Boolean = false
)
