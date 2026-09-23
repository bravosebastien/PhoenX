package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.theme.LocalAppTheme
import kotlin.math.roundToInt

/**
 * PhareFloatingBubble (v12.8)
 * Bulle flottante "Le Phare" déplaçable permettant d'ouvrir la liste des vidéos de présentation.
 */
@Composable
fun PhareFloatingBubble(
    initialX: Float?,
    initialY: Float?,
    onPositionChanged: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val bubbleSize = 56.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val maxHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
        val bubbleSizePx = with(density) { bubbleSize.toPx() }

        // Position par défaut : en haut à droite (distinct de l'Assistant IA en bas à droite)
        var offsetX by remember {
            val savedX = initialX ?: (maxWidthPx - bubbleSizePx - 24f)
            mutableFloatStateOf(savedX.coerceIn(0f, maxWidthPx - bubbleSizePx))
        }
        var offsetY by remember {
            val savedY = initialY ?: with(density) { 140.dp.toPx() }
            mutableFloatStateOf(savedY.coerceIn(0f, maxHeightPx - bubbleSizePx))
        }

        LaunchedEffect(maxWidthPx, maxHeightPx) {
            offsetX = offsetX.coerceIn(0f, maxWidthPx - bubbleSizePx)
            offsetY = offsetY.coerceIn(0f, maxHeightPx - bubbleSizePx)
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(bubbleSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(theme.backgroundColor)
                    .border(1.2.dp, accent.copy(alpha = 0.6f), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onClick() })
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                onPositionChanged(offsetX, offsetY)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx - bubbleSizePx)
                                offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxHeightPx - bubbleSizePx)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LightMode,
                    contentDescription = "Le Phare",
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
