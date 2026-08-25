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

    // RÉINTRODUCTION AMBIANCE GLOBALE v9.4.27 (Migration v48)
    val transmissionBackgroundId: String = "classic_ivory",
    val transmissionFontId: String = "playfair_display",

    // v9.4.29 : Affichage des photos de proches dans le livre
    val showPersonPhotos: Boolean = false
)
