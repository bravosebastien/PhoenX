package com.example.phoenx.data.sync

import com.example.phoenx.data.local.CreatorProfileEntity

/**
 * Extension pour convertir un CreatorProfileEntity (Room) en Map pour Firestore.
 * (v9.1 / v9.4.27)
 */
fun CreatorProfileEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "bio" to bio,
        "profession" to profession,
        "hasSiblings" to hasSiblings,
        "siblingsDetail" to siblingsDetail,
        "hasChildren" to hasChildren,
        "childrenDetail" to childrenDetail,
        "hobbies" to hobbies,
        "height" to height,
        "weight" to weight,
        "eyeColor" to eyeColor,
        "hairColor" to hairColor,
        "updatedAt" to updatedAt
    )
}
