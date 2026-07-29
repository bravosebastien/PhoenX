package com.example.phoenx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

data class Ripple(val position: Offset, val startTime: Long, val color: Color)

@Stable
class RippleTrailState {
    val ripples = mutableStateListOf<Ripple>()
    private var lastCreationTime = 0L

    fun addRipple(position: Offset, accent: Color) {
        val now = System.currentTimeMillis()
        if (now - lastCreationTime > 25) {
            ripples.add(Ripple(position, now, accent))
            lastCreationTime = now
        }
    }

    fun updateRipples(now: Long) {
        val iterator = ripples.iterator()
        while (iterator.hasNext()) {
            val ripple = iterator.next()
            if (now - ripple.startTime > 1000) {
                iterator.remove()
            }
        }
    }
}

/**
 * Modifier pour détecter les touches sans les intercepter (Initial pass).
 */
fun Modifier.rippleTrailDetection(state: RippleTrailState, accent: Color): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            // On observe au passage INITIAL pour voir l'event avant qu'il ne soit consommé
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.first()
            
            // On ne consomme RIEN ici
            state.addRipple(change.position, accent)
        }
    }
}

@Composable
fun RippleTrailOverlay(state: RippleTrailState) {
    // Recompose trigger fluide
    var frameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (currentCoroutineContext().isActive) {
            withFrameNanos { frameTime = it / 1_000_000 }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Force le redessin à chaque frame sans warning (v9.2.7)
        frameTime.let { }
        val now = System.currentTimeMillis()
        
        state.updateRipples(now)
        
        state.ripples.forEach { ripple ->
            val age = now - ripple.startTime
            val progress = age / 1000f
            val alpha = (1f - progress) * 0.4f
            val radius = 10.dp.toPx() + (progress * 150.dp.toPx())
            
            drawCircle(
                color = ripple.color.copy(alpha = alpha),
                radius = radius,
                center = ripple.position,
                style = Stroke(width = (1.5.dp.toPx() * (1f - progress)))
            )
        }
    }
}
