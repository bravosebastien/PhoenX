package com.example.phoenx.data.sync

import com.example.phoenx.data.local.StandaloneMediaEntity
import com.google.firebase.firestore.Blob

fun StandaloneMediaEntity.toFirestoreMap(): Map<String, Any?> {
    val needsEncryption = type == "TEXT_EXCERPT" || type == "PHOTO"
    
    return mapOf(
        "uid" to creatorUid,
        "type" to type,
        "title" to title,
        "description" to description, // v9.3.3
        "content" to if (needsEncryption) {
            val bytes = android.util.Base64.decode(content, android.util.Base64.DEFAULT)
            Blob.fromBytes(bytes)
        } else {
            content
        },
        "recipientIds" to recipientIds.split(",").filter { it.isNotBlank() }.map { it.trim() },
        "createdAt" to createdAt
    )
}
