package com.example.phoenx.ui.util

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

object NavigationAnimations {

    private val transitions = listOf("FADE", "DEPTH", "PAGE")
    private var currentMode = transitions.random()

    fun randomize() {
        currentMode = transitions.random()
    }

    fun getEnterTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
        return when (currentMode) {
            "FADE" -> fadeIn(animationSpec = tween(500))
            "DEPTH" -> fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.92f, animationSpec = tween(500))
            "PAGE" -> scope.slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
            else -> fadeIn()
        }
    }

    fun getExitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
        return when (currentMode) {
            "FADE" -> fadeOut(animationSpec = tween(500))
            "DEPTH" -> fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 1.08f, animationSpec = tween(500))
            "PAGE" -> scope.slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
            else -> fadeOut()
        }
    }

    fun getPopEnterTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
        return when (currentMode) {
            "FADE" -> fadeIn(animationSpec = tween(500))
            "DEPTH" -> fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 1.08f, animationSpec = tween(500))
            "PAGE" -> scope.slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
            else -> fadeIn()
        }
    }

    fun getPopExitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
        return when (currentMode) {
            "FADE" -> fadeOut(animationSpec = tween(500))
            "DEPTH" -> fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.92f, animationSpec = tween(500))
            "PAGE" -> scope.slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
            else -> fadeOut()
        }
    }
}
