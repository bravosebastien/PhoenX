package com.example.phoenx.domain.util

import java.security.MessageDigest

object EnigmaUtils {
    /**
     * Normalise et hache une réponse (SHA-256).
     * Normalisation : trim + lowercase.
     */
    fun hashAnswer(answer: String?): String? {
        if (answer.isNullOrBlank()) return null
        return MessageDigest
            .getInstance("SHA-256")
            .digest(answer.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
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
