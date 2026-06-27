package com.example.phoenx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

/**
 * OpenBookCanvas (Signature PHOEN-X 5.0)
 * Dessine un livre ouvert stylisé en 2D avec perspective légère.
 */
@Composable
fun OpenBookCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f

        // 1. OMBRE PORTÉE DU LIVRE
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x40000000), Color(0x00000000)),
                center = Offset(centerX, h * 0.92f),
                radius = w * 0.45f
            ),
            topLeft = Offset(w * 0.1f, h * 0.80f),
            size = Size(w * 0.8f, h * 0.25f)
        )

        // 2. COUVERTURE DU LIVRE
        val bookPath = Path().apply {
            moveTo(centerX, h * 0.72f)
            lineTo(w * 0.05f, h * 0.68f)
            lineTo(w * 0.05f, h * 0.12f)
            lineTo(centerX, h * 0.08f)
            close()
        }
        drawPath(path = bookPath, color = Color(0xFF1C1410))

        val bookPathRight = Path().apply {
            moveTo(centerX, h * 0.72f)
            lineTo(w * 0.95f, h * 0.68f)
            lineTo(w * 0.95f, h * 0.12f)
            lineTo(centerX, h * 0.08f)
            close()
        }
        drawPath(path = bookPathRight, color = Color(0xFF241A0E))

        // 3. PAGES GAUCHE
        val pageLeft = Path().apply {
            moveTo(centerX - 8f, h * 0.70f)
            lineTo(w * 0.07f, h * 0.66f)
            lineTo(w * 0.07f, h * 0.14f)
            lineTo(centerX - 8f, h * 0.10f)
            close()
        }
        drawPath(
            path = pageLeft,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF5EFE6), Color(0xFFEDE5D8)),
                start = Offset(w * 0.07f, 0f),
                end = Offset(centerX, 0f)
            )
        )

        // 4. PAGES DROITE
        val pageRight = Path().apply {
            moveTo(centerX + 8f, h * 0.70f)
            lineTo(w * 0.93f, h * 0.66f)
            lineTo(w * 0.93f, h * 0.14f)
            lineTo(centerX + 8f, h * 0.10f)
            close()
        }
        drawPath(
            path = pageRight,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF2EDE8), Color(0xFFE8DFD0)),
                start = Offset(centerX, 0f),
                end = Offset(w * 0.93f, 0f)
            )
        )

        // 5. RELIURE CENTRALE
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8B5A1E), Color(0xFFC97B3A), Color(0xFF8B5A1E))
            ),
            start = Offset(centerX, h * 0.08f),
            end = Offset(centerX, h * 0.72f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // 6. LIGNES DE RÉGLURE (Page Droite)
        val lineColor = Color(0x25C97B3A)
        val lineStart = centerX + 20f
        val lineEnd = w * 0.90f
        val lineTop = h * 0.18f
        val lineSpacing = (h * 0.70f - h * 0.18f) / 10f

        for (i in 0..9) {
            val y = lineTop + i * lineSpacing
            drawLine(
                color = lineColor,
                start = Offset(lineStart, y),
                end = Offset(lineEnd, y),
                strokeWidth = 1f
            )
        }
    }
}
