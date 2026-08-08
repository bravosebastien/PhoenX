package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.theme.LocalAppTheme
import kotlin.math.roundToInt

/**
 * FloatingAssistantBubble (v9.4.25)
 * Bulle flottante déplaçable permettant d'ouvrir l'assistant IA.
 */
@Composable
fun FloatingAssistantBubble(
    initialX: Float?,
    initialY: Float?,
    onPositionChanged: (Float, Float) -> Unit,
    onClick: () -> Unit,
    showWelcomeTooltip: Boolean = false // v9.4.27
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    // Taille de la bulle
    val bubbleSize = 56.dp
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val maxHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
        val bubbleSizePx = with(density) { bubbleSize.toPx() }

        // Position par défaut : en bas à droite (v9.4.25)
        var offsetX by remember { 
            mutableFloatStateOf(initialX ?: (maxWidthPx - bubbleSizePx - 100f)) 
        }
        var offsetY by remember { 
            mutableFloatStateOf(initialY ?: (maxHeightPx - bubbleSizePx - 250f))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(bubbleSize)
        ) {
            // LE TOOLTIP PROACTIF (v9.4.27)
            androidx.compose.animation.AnimatedVisibility(
                visible = showWelcomeTooltip,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-48).dp)
            ) {
                Surface(
                    color = accent,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.widthIn(max = 180.dp).clickable { onClick() }
                ) {
                    Text(
                        text = "Besoin d'aide pour tes premiers pas ? 👋",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(theme.backgroundColor)
                    .border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
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
                                // Contraintes pour rester à l'écran
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx - bubbleSizePx)
                                offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxHeightPx - bubbleSizePx)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Icône IA (v9.4.25 Placeholder)
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Assistant IA",
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
