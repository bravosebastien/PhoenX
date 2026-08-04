package com.example.phoenx.domain.model

/**
 * SimplifiedPerson (v9.4.26)
 * Modèle unifié pour regrouper les personnes issues des différentes sources
 * (Arbre, Destinataires, Témoins, Dépositaires).
 */
data class SimplifiedPerson(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val sourceType: String, // "arbre_livre", "destinataire", "temoin", "depositaire"
    val relationship: String? = null,
    val isMe: Boolean = false
)

/**
 * Extension pour unifier les sources (v9.4.26)
 */
fun List<com.example.phoenx.data.local.PersonEntity>.toSimplified() = map {
    SimplifiedPerson(
        id = it.id,
        name = it.firstName + (it.lastName?.let { l -> " $l" } ?: ""),
        photoUrl = it.imagePath,
        sourceType = "arbre_livre",
        relationship = it.relationship
    )
}

fun List<com.example.phoenx.data.local.RecipientEntity>.toSimplifiedRecipient() = map {
    SimplifiedPerson(
        id = it.id,
        name = it.name,
        photoUrl = it.photoUrl,
        sourceType = "destinataire",
        relationship = it.relationship
    )
}

fun List<com.example.phoenx.data.local.WitnessEntity>.toSimplifiedWitness() = map {
    SimplifiedPerson(
        id = it.id,
        name = it.name,
        photoUrl = it.photoUrl,
        sourceType = "temoin"
    )
}

fun List<com.example.phoenx.data.local.DepositaryEntity>.toSimplifiedDepositary() = map {
    SimplifiedPerson(
        id = it.id,
        name = it.name,
        photoUrl = it.photoUrl,
        sourceType = "depositaire",
        relationship = if (it.role == "primary") "Gardien Principal" else "Gardien Secondaire"
    )
}
