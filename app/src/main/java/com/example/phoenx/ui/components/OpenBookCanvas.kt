package com.example.phoenx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

/**
 * OpenBookCanvas (Signature PHOEN-X 5.0 - Version Organique)
 * Dessine un livre ouvert avec des pages courbes et une perspective réaliste.
 */
@Composable
fun OpenBookCanvas(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        // On laisse une marge de sécurité de 8dp pour que les coins ne touchent jamais le bord
        val margin = 8.dp.toPx()
        val w = size.width - (margin * 2)
        val h = size.height - (margin * 2)
        val centerX = size.width / 2f
        val topY = margin
        val bottomY = size.height - margin

        // 1. OMBRE PORTÉE (Plus diffuse)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x30000000), Color(0x00000000)),
                center = Offset(centerX, bottomY),
                radius = w * 0.5f
            ),
            topLeft = Offset(margin, h * 0.85f),
            size = Size(w, h * 0.2f)
        )

        // 2. COUVERTURE (Légèrement plus large que les pages)
        val coverPath = Path().apply {
            moveTo(centerX, topY + 10f)
            quadraticTo(centerX * 0.5f, topY, margin, topY + 20f) // Bord haut gauche
            lineTo(margin, bottomY - 10f)
            quadraticTo(centerX * 0.5f, bottomY, centerX, bottomY - 20f) // Bord bas gauche
            close()
        }
        drawPath(path = coverPath, color = Color(0xFF1C1410))

        val coverPathRight = Path().apply {
            moveTo(centerX, topY + 10f)
            quadraticTo(centerX * 1.5f, topY, size.width - margin, topY + 20f)
            lineTo(size.width - margin, bottomY - 10f)
            quadraticTo(centerX * 1.5f, bottomY, centerX, bottomY - 20f)
            close()
        }
        drawPath(path = coverPathRight, color = Color(0xFF241A0E))

        // 3. PAGES GAUCHE (Courbées avec Bézier pour le galbe)
        val pageL = Path().apply {
            moveTo(centerX - 4f, bottomY - 25f)
            quadraticTo(centerX * 0.5f, bottomY - 5f, margin + 15f, bottomY - 20f) // Bas galbé
            lineTo(margin + 15f, topY + 30f)
            quadraticTo(centerX * 0.5f, topY + 15f, centerX - 4f, topY + 25f) // Haut galbé
            close()
        }
        drawPath(
            path = pageL,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFEDE5D8), Color(0xFFF5EFE6)),
                start = Offset(margin, 0f),
                end = Offset(centerX, 0f)
            )
        )

        // 4. PAGES DROITE (C'est ici qu'on écrit)
        val pageR = Path().apply {
            moveTo(centerX + 4f, bottomY - 25f)
            quadraticTo(centerX * 1.5f, bottomY - 5f, size.width - margin - 15f, bottomY - 20f)
            lineTo(size.width - margin - 15f, topY + 30f)
            quadraticTo(centerX * 1.5f, topY + 15f, centerX + 4f, topY + 25f)
            close()
        }
        drawPath(
            path = pageR,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF2EDE8), Color(0xFFE8DFD0)),
                start = Offset(centerX, 0f),
                end = Offset(size.width - margin, 0f)
            )
        )

        // 5. RELIURE (L'âme du livre)
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF8B5A1E), Color(0xFFC97B3A), Color(0xFF5C3A10))
            ),
            start = Offset(centerX, topY + 15f),
            end = Offset(centerX, bottomY - 20f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }
}
