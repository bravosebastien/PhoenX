package com.example.phoenx.data.ai

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OnDeviceAIManager (Signature PHOEN-X 5.0)
 * Gère l'analyse IA locale via Gemini Nano (AICore).
 * RÈGLE DE SÉCURITÉ : Analyse AVANT le chiffrement, ne stocke JAMAIS le texte brut.
 */

data class LocalAnalysis(
    val summary: String,
    val tags: List<String>,
    val emotionalTone: String,
    val lifePeriod: String,
)

@Singleton
class OnDeviceAIManager @Inject constructor() {

    /**
     * Analyse le contenu localement sans envoyer de données au cloud.
     * Fallback vers une analyse par mots-clés si AICore n'est pas prêt.
     */
    suspend fun analyzeLocally(rawContent: String): LocalAnalysis = withContext(Dispatchers.Default) {
        // En 2026, Gemini Nano est accessible via AICore.
        // Ici nous simulons l'appel au modèle local pour le MVP.
        
        val summary = generateSummary(rawContent)
        val tags = extractTags(rawContent)
        val tone = detectTone(rawContent)
        val period = determineLifePeriod(rawContent)

        LocalAnalysis(summary, tags, tone, period)
    }

    private fun generateSummary(text: String): String {
        // Simulation Gemini Nano : Résumé de 20 mots max
        val words = text.split(" ")
        return if (words.size > 15) {
            words.asSequence().take(n = 15).joinToString(separator = " ") + "..."
        } else {
            text
        }
    }

    private fun extractTags(text: String): List<String> {
        val possibleTags = listOf("Famille", "Amour", "Travail", "Santé", "Peur", "Joie", "Regret", "Sagesse", "Enfance")
        return possibleTags.filter { text.contains(it, ignoreCase = true) }.take(3)
    }

    private fun detectTone(text: String): String {
        val tones = mapOf(
            "heureux" to "Joyeux",
            "triste" to "Mélancolique",
            "peur" to "Inquiet",
            "merci" to "Reconnaissant",
            "pense" to "Apaisé"
        )
        return tones.entries.firstOrNull { text.contains(it.key, ignoreCase = true) }?.value ?: "Nostalgique"
    }

    private fun determineLifePeriod(text: String): String {
        return when {
            text.contains("enfant", ignoreCase = true) || text.contains("petit", ignoreCase = true) -> "Enfance"
            text.contains("école", ignoreCase = true) || text.contains("lycée", ignoreCase = true) -> "Adolescence"
            text.contains("travail", ignoreCase = true) || text.contains("bureau", ignoreCase = true) -> "Adulte"
            else -> "Actuel"
        }
    }
}
