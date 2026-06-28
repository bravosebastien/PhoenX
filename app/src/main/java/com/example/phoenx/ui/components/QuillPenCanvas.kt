package com.example.phoenx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * QuillPenCanvas (Signature PHOEN-X 5.0)
 * Dessine une plume dorée qui suit précisément le curseur de texte.
 */
@Composable
fun QuillPenCanvas(
    tipOffset: Offset, // Position exacte de la pointe (X, Y)
    isWriting: Boolean,
    modifier: Modifier = Modifier,
) {
    val tremble by animateFloatAsState(
        targetValue = if (isWriting) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tremble"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "plume_motion")
    val vibrate by infiniteTransition.animateFloat(
        initialValue = -0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "vibrate"
    )

    Canvas(modifier = modifier) {
        if (tipOffset == Offset(0f, 0f) && !isWriting) return@Canvas

        val tipX = tipOffset.x + (vibrate * tremble)
        val tipY = tipOffset.y

        // 1. CORPS DE LA PLUME
        // On incline la plume à 45 degrés pour le réalisme
        val angle = -45f * (PI / 180f).toFloat()
        val plumeLength = size.height * 0.4f
        val plumeWidth = 24f
        
        val hautX = tipX - cos(angle) * plumeLength
        val hautY = tipY - sin(angle) * plumeLength
        
        val perpX = -sin(angle)
        val perpY = cos(angle)

        val plumePath = Path().apply {
            moveTo(tipX, tipY)
            // Dessin asymétrique pour simuler une vraie plume d'oie
            quadraticTo(
                tipX - cos(angle) * 20f, tipY - sin(angle) * 20f,
                hautX - perpX * plumeWidth, hautY - perpY * plumeWidth
            )
            quadraticTo(hautX, hautY - 30f, hautX + perpX * plumeWidth, hautY - perpY * plumeWidth)
            quadraticTo(
                tipX - cos(angle) * 30f, tipY - sin(angle) * 5f,
                tipX, tipY
            )
            close()
        }

        drawPath(
            path = plumePath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8B5A1E), Color(0xFFC97B3A), Color(0xFFE8A85F), Color(0xFFC97B3A)),
                start = Offset(tipX, tipY),
                end = Offset(hautX, hautY)
            )
        )

        // 2. LA POINTE (Nib)
        drawLine(
            color = Color(0xFF2A1F10),
            start = Offset(tipX, tipY),
            end = Offset(tipX - cos(angle) * 10f, tipY - sin(angle) * 10f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        // 3. GOUTTE D'ENCRE (Seulement lors de l'écriture)
        if (isWriting) {
            drawCircle(
                color = Color(0xFFC97B3A).copy(alpha = 0.6f),
                radius = 3f,
                center = Offset(tipX, tipY + 2f)
            )
        }
    }
}
