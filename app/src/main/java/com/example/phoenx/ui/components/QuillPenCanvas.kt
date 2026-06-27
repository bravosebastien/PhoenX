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
 * Dessine une plume dorée animée qui vibre pendant l'écriture.
 */
@Composable
fun QuillPenCanvas(
    progress: Float, // 0f à 1f sur la ligne
    currentLine: Int,
    isWriting: Boolean,
    modifier: Modifier = Modifier,
) {
    val tremble by animateFloatAsState(
        targetValue = if (isWriting) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tremble"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tremble_infinite")
    val trembleX by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(80, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "trembleX"
    )
    
    val inkPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "inkPulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Coordonnées de la pointe
        val tipX = (w * 0.07f) + (w * 0.86f * progress) + (trembleX * tremble)
        val tipY = (h * 0.18f) + (currentLine * h * 0.052f)

        // 1. CORPS DE LA PLUME (Incliné)
        val angle = -40f * (PI / 180f).toFloat()
        val plumeLength = h * 0.35f
        val plumeWidth = h * 0.06f
        val hautX = tipX - cos(angle) * plumeLength
        val hautY = tipY - sin(angle) * plumeLength
        val perpX = -sin(angle)
        val perpY = cos(angle)

        val plumePath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(hautX - perpX * plumeWidth * 0.5f, hautY - perpY * plumeWidth * 0.5f)
            quadraticTo(hautX, hautY - h * 0.04f, hautX + perpX * plumeWidth * 0.5f, hautY - perpY * plumeWidth * 0.5f)
            close()
        }

        drawPath(
            path = plumePath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8B5A1E), Color(0xFFC97B3A), Color(0xFFE8A85F), Color(0xFFC97B3A)),
                start = Offset(hautX - perpX * plumeWidth, hautY),
                end = Offset(hautX + perpX * plumeWidth, hautY)
            )
        )

        // 2. POINTE MÉTALLIQUE
        val nibLength = h * 0.06f
        drawLine(
            brush = Brush.linearGradient(colors = listOf(Color(0xFF9B9590), Color(0xFFF2EDE8), Color(0xFF9B9590))),
            start = Offset(tipX, tipY),
            end = Offset(tipX - cos(angle) * nibLength, tipY - sin(angle) * nibLength),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // 3. GOUTTE D'ENCRE
        if (isWriting) {
            drawCircle(
                color = Color(0xFFC97B3A).copy(alpha = 0.8f),
                radius = 4f * inkPulse,
                center = Offset(tipX, tipY + 2f)
            )
        }
    }
}
