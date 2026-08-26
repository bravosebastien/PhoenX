package com.example.phoenx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String? = null,
    val relationship: String? = null, // ex: "compagne", "cousin"
    val distinctionType: String? = null, // "nom_famille", "surnom", "ville", "autre"
    val distinctionValue: String? = null,
    val imagePath: String? = null, // v8.9.9 : Chemin vers le portrait Cameo
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending",

    // ÉCRAN PERSONNAGES v9.0
    val height: Int? = null,
    val weight: Int? = null,
    val eyeColor: String? = null,
    val hairColor: String? = null,
    val clothingStyle: String? = null,
    val profession: String? = null,
    val hasChildren: Boolean? = null,
    val relationshipDetail: String? = null,
    val characterType: String? = "HUMAN", // v9.1 : "HUMAN" | "ANIMAL"

    // ARBRE GÉNÉALOGIQUE v9.4.22
    val parentIds: String = "", // Format CSV sécurisé : ",ID1,ID2,"
    val isDeceased: Boolean = false,
    val biography: String = "",
    val isReparented: Boolean = false, // v9.4.23 : Marqueur pour remontée automatique
    val reparentedRelationLabel: String? = null, // v9.4.23 : Libellé de lien personnalisé

    // LES RENCONTRES v9.5.0
    val categories: String = ",FAMILY,", // Format CSV sécurisé : ",FAMILY,ENCOUNTER,"
    val introducedById: String? = null,
    val encounterAge: Int? = null,
    val encounterLocationId: String? = null,
    val encounterLocationLabel: String? = null,
    val linkNature: String? = null, // ami, mentor, collègue, amour, etc.
    val linkStatus: String? = null, // PRESENT, LOST, PASSED
    val visibility: String = "PUBLIC" // PUBLIC | PRIVATE
)
