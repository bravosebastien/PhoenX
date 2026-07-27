package com.example.phoenx.domain.model

/**
 * Représente un rôle externe (Dépositaire, Témoin, Destinataire) 
 * occupé par l'utilisateur pour un autre Créateur.
 */
data class UserRole(
    val creatorId: String = "",
    val creatorName: String = "Votre proche",
    val role: String = "", // "depositary" | "witness" | "recipient"
    val status: String = "", // "invited" | "active" | "submitted"
    val label: String = "", // ex: "Gardien de confiance", "Héritier"
    val photoUrl: String? = null, // v9.2.2: Creator's profile photo
    val sourceId: String? = null, // ID du document source
    val joinedAt: Long? = null,
    val migratedAt: Long? = null
)
