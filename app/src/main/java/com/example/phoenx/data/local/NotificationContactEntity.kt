package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_contacts")
data class NotificationContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val relationship: String,
    val addedAt: Long
)
