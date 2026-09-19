package com.example.phoenx.domain.util

import java.security.MessageDigest
import java.text.Normalizer

object EnigmaUtils {
    /**
     * Normalise et hache une réponse (SHA-256).
     * Normalisation : trim + lowercase.
     */
    fun hashAnswer(answer: String?, type: String? = null): String? {
        if (answer.isNullOrBlank()) return null
        return MessageDigest
            .getInstance("SHA-256")
            .digest(normalizeAnswer(answer, type).toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Nettoie la réponse selon le type (v12.7.6).
     */
    fun normalizeAnswer(answer: String, type: String?): String {
        return when (type) {
            "NUMBER" -> {
                // Ne garder que les chiffres
                answer.filter { it.isDigit() }
            }
            "WORD" -> {
                // v12.7.6 : plus tolérant (sans accents, espaces simples)
                val temp = Normalizer.normalize(answer.trim().lowercase(), Normalizer.Form.NFD)
                val withoutAccents = Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
                Regex("\\s+").replace(withoutAccents, " ")
            }
            else -> {
                // null (Legacy) : comportement historique strict pour ne pas casser l'existant
                answer.trim().lowercase()
            }
        }
    }

    /**
     * Vérifie si une chaîne ressemble à un hash SHA-256 (64 chars hex).
     */
    fun isAlreadyHashed(text: String?): Boolean {
        if (text == null) return false
        val regex = Regex("^[a-f0-9]{64}$", RegexOption.IGNORE_CASE)
        return regex.matches(text)
    }
}
