package com.example.phoenx.data.sync

import com.example.phoenx.data.local.PersonMediaEntity
import com.google.firebase.firestore.DocumentSnapshot

fun PersonMediaEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "personId" to personId,
        "mediaPath" to mediaPath,
        "mediaType" to mediaType,
        "capturedAt" to capturedAt
    )
}

fun DocumentSnapshot.toPersonMediaEntity(): PersonMediaEntity {
    return PersonMediaEntity(
        id = id,
        personId = getString("personId") ?: "",
        mediaPath = getString("mediaPath") ?: "",
        mediaType = getString("mediaType") ?: "PHOTO",
        capturedAt = getLong("capturedAt") ?: System.currentTimeMillis(),
        syncStatus = "synced"
    )
}
