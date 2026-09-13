package com.example.phoenx.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.random.Random

object NudgePhrases {
    /**
     * Retourne une phrase au hasard différente de la précédente.
     */
    fun getRandomPhrase(context: android.content.Context, lastPhrase: String? = null): String {
        val phrases = context.resources.getStringArray(com.example.phoenx.R.array.nudge_phrases).toList()
        val filtered = if (lastPhrase != null) phrases.filter { it != lastPhrase } else phrases
        return filtered[Random.nextInt(filtered.size)]
    }
}
