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
}
