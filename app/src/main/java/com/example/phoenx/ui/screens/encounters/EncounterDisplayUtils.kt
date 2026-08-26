package com.example.phoenx.ui.screens.encounters

/**
 * EncounterDisplayUtils (v9.6.0)
 * Logique d'affichage pour le tiroir des Rencontres.
 */

fun displayLinkNature(raw: String?): String {
    return when(raw?.trim()?.lowercase()) {
        "amour", "compagnon" -> "Partenaire"
        null, "" -> "non renseigné"
        else -> raw.trim()
    }
}
