package com.example.phoenx.ui.screens.library.archive

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.phoenx.ui.theme.AppThemeState

/**
 * CulturePainter — Dessine les objets liés aux médias culturels.
 * v8.9.8 : Intégration du thémage dynamique.
 */

// ═══════════════════════════════════
// BIBLIOTHÈQUE — Livres sur étagère
// ═══════════════════════════════════
fun DrawScope.drawBooks(theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor
    
    val bookColors = listOf(
        Color(0xFF8B2020), Color(0xFF1A3A5C), Color(0xFF2A4A1E),
        Color(0xFF3A2A0A), Color(0xFF4A1A3A), Color(0xFF1A2A3A),
        Color(0xFF5A3A0A), Color(0xFF2A1A4A)
    )
    val bookWidths = listOf(0.08f, 0.06f, 0.09f, 0.07f, 0.08f, 0.06f, 0.09f, 0.07f)
    var currentX = 0.06f

    bookWidths.forEachIndexed { index, width ->
        val bookColor = bookColors[index % bookColors.size]
        val bookHeight = 0.55f + (index % 3) * 0.08f
        val bookBottom = h * 0.88f
        val bookTop = bookBottom - h * bookHeight
        val bookLeft = w * currentX
        val bookRight = bookLeft + w * width

        // Corps du livre
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    bookColor.copy(alpha = 0.7f),
                    bookColor,
                    bookColor.copy(alpha = 0.85f)
                ),
                start = Offset(bookLeft, 0f),
                end = Offset(bookRight, 0f)
            ),
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookRight - bookLeft, bookBottom - bookTop)
        )

        // Tranche dorée (v8.9.8 : Utilise accentColor)
        drawRect(
            color = accent.copy(alpha = 0.6f),
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookRight - bookLeft, 3f)
        )

        // Séparation entre livres (v8.9.8 : Utilise contentColor)
        drawLine(
            color = theme.contentColor.copy(alpha = 0.2f),
            start = Offset(bookRight, bookTop),
            end = Offset(bookRight, bookBottom),
            strokeWidth = 1.5f
        )

        currentX += width + 0.01f
    }
}

// ═══════════════════════════════════
// DISCOTHÈQUE — Vinyles
// ═══════════════════════════════════
fun DrawScope.drawVinyls(theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor
    
    val vinylColors = listOf(
        Color(0xFF1A1A1F), Color(0xFF2A1A0A), Color(0xFF0A1A2A)
    )

    vinylColors.forEachIndexed { index, color ->
        val centerX = w * (0.25f + index * 0.28f)
        val centerY = h * 0.55f
        val radius = minOf(w, h) * 0.22f

        // Corps du vinyle
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY)
        )

        // Sillons du vinyle
        for (r in 3..8) {
            drawCircle(
                color = theme.contentColor.copy(alpha = 0.1f),
                radius = radius * (r / 10f),
                center = Offset(centerX, centerY),
                style = Stroke(width = 0.8f)
            )
        }

        // Étiquette centrale (v8.9.8 : Utilise accentColor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent, accent.copy(alpha = 0.7f)),
                center = Offset(centerX, centerY),
                radius = radius * 0.25f
            ),
            radius = radius * 0.25f,
            center = Offset(centerX, centerY)
        )

        // Reflet sur le vinyle
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f),
                    Color.Transparent
                )
            ),
            startAngle = 200f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = radius * 0.3f)
        )
    }
}

// ═══════════════════════════════════
// VIDÉOTHÈQUE — Cassettes VHS
// ═══════════════════════════════════
fun DrawScope.drawCassettes(theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor
    
    val cassetteColors = listOf(
        Color(0xFF1A1A2A), Color(0xFF1A2A1A), Color(0xFF2A1A1A), Color(0xFF1A1A1A)
    )

    cassetteColors.forEachIndexed { index, color ->
        val left = w * (0.05f + index * 0.24f)
        val top = h * 0.25f
        val casW = w * 0.20f
        val casH = h * 0.50f

        // Corps de la cassette
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(casW, casH),
            cornerRadius = CornerRadius(4f)
        )

        // Fenêtre de la cassette
        drawRoundRect(
            color = Color(0xFF2E2E35),
            topLeft = Offset(left + casW * 0.1f, top + casH * 0.15f),
            size = Size(casW * 0.8f, casH * 0.35f),
            cornerRadius = CornerRadius(3f)
        )

        // Bobines
        listOf(0.28f, 0.72f).forEach { bobineX ->
            drawCircle(
                color = Color(0xFF1A1A1F),
                radius = casW * 0.15f,
                center = Offset(left + casW * bobineX, top + casH * 0.33f)
            )
            drawCircle(
                color = Color(0xFF3A3A45),
                radius = casW * 0.07f,
                center = Offset(left + casW * bobineX, top + casH * 0.33f)
            )
        }

        // Étiquette
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(theme.contentColor.copy(alpha = 0.05f), theme.contentColor.copy(alpha = 0.1f))
            ),
            topLeft = Offset(left + casW * 0.08f, top + casH * 0.55f),
            size = Size(casW * 0.84f, casH * 0.35f)
        )

        // Lignes d'étiquette (v8.9.8 : Utilise accentColor)
        repeat(3) { lineIndex ->
            drawLine(
                color = accent.copy(alpha = 0.3f),
                start = Offset(
                    left + casW * 0.12f,
                    top + casH * (0.62f + lineIndex * 0.08f)
                ),
                end = Offset(
                    left + casW * 0.88f,
                    top + casH * (0.62f + lineIndex * 0.08f)
                ),
                strokeWidth = 1f
            )
        }
    }
}
