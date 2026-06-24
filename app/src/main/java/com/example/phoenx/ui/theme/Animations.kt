package com.example.phoenx.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

object PhoenXAnimations {
    // Animation de transition organique (ADN Phoen-X)
    val OrganicFadeIn = fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing))
    val OrganicFadeOut = fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing))

    // Ritual de dépôt : Glissement vertical lent comme une lettre dans une fente
    val RitualDepositEnter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 4 }
    val RitualDepositExit = fadeOut(tween(600)) + slideOutVertically(tween(600)) { -it / 4 }

    // Animation de pulsation pour la "matière" (Mode Nuit / 3h du matin)
    val NightPulse = tween<Float>(durationMillis = 2000, easing = FastOutSlowInEasing)

    // Animation PressDown pour l'effet de poids/matière
    @Composable
    fun pressScale(pressed: Boolean): Float {
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "press_scale"
        )
        return scale
    }
}
