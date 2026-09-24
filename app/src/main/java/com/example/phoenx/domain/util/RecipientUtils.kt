package com.example.phoenx.domain.util

/**
 * Utilitaires centralisés pour le traitement des identifiants destinataires (v12.8)
 */
object RecipientUtils {

    /**
     * Nettoie et dédoublonne une chaîne CSV d'identifiants destinataires.
     */
    fun cleanRecipientIds(idsCsv: String): String {
        return idsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
    }

    /**
     * Découpe et nettoie une chaîne CSV d'identifiants destinataires en liste d'IDs uniques.
     */
    fun parseRecipientIds(idsCsv: String): List<String> {
        return idsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
