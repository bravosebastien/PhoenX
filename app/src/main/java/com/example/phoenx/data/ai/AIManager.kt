package com.example.phoenx.data.ai

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class AIManager @Inject constructor(
    private val functions: FirebaseFunctions
) {
    /**
     * RÈGLE ABSOLUE : Ne JAMAIS envoyer de contenu chiffré à l'IA.
     * On envoie uniquement des résumés ou tags non sensibles.
     * Utilisation de gemini-3.1-flash-lite (juin 2026).
     */

    suspend fun analyzeEntrySummary(summary: String): Map<String, Any> {
        val data = hashMapOf(
            "summary" to summary,
            "model" to "gemini-3.1-flash-lite"
        )
        val result: HttpsCallableResult = functions.getHttpsCallable("analyzeEntry").call(data).await()
        @Suppress("UNCHECKED_CAST")
        return result.data as Map<String, Any>
    }

    suspend fun getBiographerQuestion(): String {
        val data = hashMapOf("model" to "gemini-3.1-flash-lite")
        val result: HttpsCallableResult = functions.getHttpsCallable("generateBiographerQuestion").call(data).await()
        return result.data as String
    }

    suspend fun generateEssencePortrait(summaries: List<String>): String {
        val data = hashMapOf(
            "summaries" to summaries,
            "model" to "gemini-3.1-flash-lite"
        )
        val result: HttpsCallableResult = functions.getHttpsCallable("generateEssencePortrait").call(data).await()
        return result.data as String
    }

    suspend fun analyzeEvolution(original: String, amendment: String): String {
        val data = hashMapOf(
            "original" to original,
            "amendment" to amendment,
            "model" to "gemini-3.1-flash-lite"
        )
        val result: HttpsCallableResult = functions.getHttpsCallable("detectThoughtEvolution").call(data).await()
        // La fonction cloud renvoie un JSON, on extrait la description
        return try {
            val map = result.data as Map<*, *>
            val transitions = map["transitions"] as List<*>
            val first = transitions.first() as Map<*, *>
            first["description"] as String
        } catch (e: Exception) {
            "Évolution de pensée détectée entre ces deux époques."
        }
    }
}
