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
        "showPersonPhotos" to showPersonPhotos,
        "updatedAt" to updatedAt
    )
}

/**
 * Extension pour convertir un DocumentSnapshot Firestore en CreatorProfileEntity (Room).
 * (v12.2 : Rapatriement lors de la réinstallation)
 */
fun com.google.firebase.firestore.DocumentSnapshot.toCreatorProfileEntity(userId: String): CreatorProfileEntity {
    val richProfile = get("richProfile") as? Map<String, Any?> ?: emptyMap()
    
    return CreatorProfileEntity(
        userId = userId,
        bio = richProfile["bio"] as? String,
        profession = richProfile["profession"] as? String,
        hasSiblings = richProfile["hasSiblings"] as? Boolean,
        siblingsDetail = richProfile["siblingsDetail"] as? String,
        hasChildren = richProfile["hasChildren"] as? Boolean,
        childrenDetail = richProfile["childrenDetail"] as? String,
        hobbies = richProfile["hobbies"] as? String,
        height = (richProfile["height"] as? Long)?.toInt(),
        weight = (richProfile["weight"] as? Long)?.toInt(),
        eyeColor = richProfile["eyeColor"] as? String,
        hairColor = richProfile["hairColor"] as? String,
        updatedAt = richProfile["updatedAt"] as? Long ?: System.currentTimeMillis(),
        syncStatus = "synced",
        
        // Root fields
        transmissionBackgroundId = getString("transmissionBackgroundId") ?: "classic_ivory",
        transmissionFontId = getString("transmissionFontId") ?: "playfair_display",
        showPersonPhotos = getBoolean("showPersonPhotos") ?: (richProfile["showPersonPhotos"] as? Boolean ?: false)
    )
}
