package com.example.phoenx.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.random.Random

object NudgePhrases {
    private val phrases = listOf(
        "Un jour, quelqu'un sourira en lisant ceci.",
        "Tu ne racontes pas ta vie — tu prépares un cadeau plein d'amour.",
        "Ce souvenir attendra patiemment le bon moment pour faire vibrer un cœur.",
        "Chaque mot déposé ici est une étincelle pour demain.",
        "Tu es en train d'écrire la plus belle des lettres à ceux que tu aimes.",
        "Rien ne se perd, tout se transmet avec tendresse.",
        "Imagine la joie de redécouvrir ce moment dans quelques années.",
        "Ta voix et tes mots sont le plus précieux des héritages.",
        "Ce fragment de vie est une graine de bonheur pour le futur.",
        "Merci de prendre soin de ta propre histoire.",
        "Ici, le temps s'arrête pour laisser place à l'essentiel.",
        "Tes proches te remercieront d'avoir gardé cette trace.",
        "Un petit mot aujourd'hui, un immense trésor demain.",
        "La vie est faite de ces petits instants que tu choisis de sceller.",
        "Ta mémoire est un pont jeté vers ceux qui viendront."
    )

    /**
     * Retourne une phrase au hasard différente de la précédente.
     */
    fun getRandomPhrase(lastPhrase: String? = null): String {
        val filtered = if (lastPhrase != null) phrases.filter { it != lastPhrase } else phrases
        return filtered[Random.nextInt(filtered.size)]
    }
}
