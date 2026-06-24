package com.example.phoenx.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

object PhoenXAnimations {
    // Animation de transition pour le fil de pensée (timeline)
    val TimelineEntryEnter = fadeIn(animationSpec = tween(600)) + 
            slideInVertically(initialOffsetY = { it / 2 })
    
    val TimelineEntryExit = fadeOut(animationSpec = tween(400)) + 
            slideOutVertically(targetOffsetY = { it / 2 })

    // Animation de rebond pour les éléments interactifs (boutons, cartes)
    fun <T> springDefault() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}
