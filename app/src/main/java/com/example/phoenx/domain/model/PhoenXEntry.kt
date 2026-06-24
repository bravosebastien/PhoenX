package com.example.phoenx.domain.model

import java.time.Instant

/**
 * Modèle de base pour une entrée dans le Fil de Pensée.
 * Toutes les données sensibles sont chiffrées.
 */
data class PhoenXEntry(
    val id: String = "",
    val creatorUid: String = "",
    val timestamp: Instant = Instant.now(),
    
    // Le Fil de Pensée par Âge
    val ageAtCreation: AgeSnapshot,
    
    // Contenu chiffré (Texte, Émotion, etc.)
    val encryptedContent: ByteArray,
    
    // Type d'entrée : PENSÉE, ÉMOTION, TRANSMISSION
    val type: EntryType = EntryType.THOUGHT,
    
    // Métadonnées non sensibles pour le tri
    val tags: List<String> = emptyList()
)

enum class EntryType {
    THOUGHT,    // Pensée brute
    EMOTION,    // État émotionnel
    LEGACY      // Message destiné à être transmis plus tard
}
