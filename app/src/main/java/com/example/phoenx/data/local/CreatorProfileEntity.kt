package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creator_profile")
data class CreatorProfileEntity(
    @PrimaryKey val userId: String,
    val bio: String? = null,
    val profession: String? = null,
    val hasSiblings: Boolean? = null,
    val siblingsDetail: String? = null,
    val hasChildren: Boolean? = null,
    val childrenDetail: String? = null,
    val hobbies: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val eyeColor: String? = null,
    val hairColor: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending",
    
    // AMBIANCE DE TRANSMISSION v9.4.27 (Migration v46)
    val transmissionBackgroundId: String? = "PAPER_IVORY",
    val transmissionFontId: String? = "MODERN"
)

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
        "transmissionBackgroundId" to transmissionBackgroundId,
        "transmissionFontId" to transmissionFontId,
        "updatedAt" to updatedAt
    )
}
