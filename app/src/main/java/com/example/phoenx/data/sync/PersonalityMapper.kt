package com.example.phoenx.data.sync

import com.example.phoenx.data.local.PersonalityEntity
import com.example.phoenx.data.local.PersonalityMediaEntity

fun PersonalityEntity.toFirestoreMap(storageUrl: String? = null): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "category" to category,
        "customCategoryLabel" to customCategoryLabel,
        "mainPhotoPath" to (storageUrl ?: mainPhotoPath),
        "biography" to biography,
        "personalComment" to personalComment,
        "createdAt" to createdAt
    )
}

fun PersonalityMediaEntity.toFirestoreMap(storageUrl: String? = null): Map<String, Any?> {
    return mapOf(
        "personalityId" to personalityId,
        "mediaPath" to (storageUrl ?: mediaPath),
        "capturedAt" to capturedAt
    )
}
