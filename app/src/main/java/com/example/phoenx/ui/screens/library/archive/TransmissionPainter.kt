package com.example.phoenx.ui.screens.library.archive

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.phoenx.ui.theme.AppThemeState
import kotlin.math.cos
import kotlin.math.sin

/**
 * TransmissionPainter — Dessine les objets liés au sacré et au secret.
 * v8.9.8 : Intégration du thémage dynamique.
 */

// ═══════════════════════════════════
// COFFRE-FORT
// ═══════════════════════════════════
fun DrawScope.drawSafe(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor

    // Corps du coffre
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF2A2A35), Color(0xFF1A1A25))
        ),
        topLeft = Offset(w * 0.08f, h * 0.12f),
        size = Size(w * 0.84f, h * 0.76f),
        cornerRadius = CornerRadius(8f)
    )

    // Rivets aux coins (v8.9.8 : Utilise accentColor)
    listOf(
        Offset(w * 0.15f, h * 0.20f), Offset(w * 0.85f, h * 0.20f),
        Offset(w * 0.15f, h * 0.80f), Offset(w * 0.85f, h * 0.80f)
    ).forEach { rivet ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent, accent.copy(alpha = 0.6f)),
                center = rivet,
                radius = w * 0.025f
            ),
            radius = w * 0.025f,
            center = rivet
        )
    }

    // Roue de combinaison
    val wheelCenter = Offset(w * 0.5f, h * 0.45f)
    val wheelRadius = minOf(w, h) * 0.22f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3A3A45), Color(0xFF1A1A25)),
            center = wheelCenter,
            radius = wheelRadius
        ),
        radius = wheelRadius,
        center = wheelCenter
    )
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                accent.copy(alpha = 0.7f), accent,
                accent.copy(alpha = 0.8f), accent, accent.copy(alpha = 0.7f)
            ),
            center = wheelCenter
        ),
        radius = wheelRadius,
        center = wheelCenter,
        style = Stroke(width = 4f)
    )

    // Encoches de la roue (v8.9.8 : Utilise accentColor)
    repeat(12) { i ->
        val angle = (i * 30f) * (Math.PI / 180f).toFloat()
        val startR = wheelRadius * 0.85f
        val endR = wheelRadius * 0.95f
        drawLine(
            color = accent,
            start = Offset(
                wheelCenter.x + cos(angle) * startR,
                wheelCenter.y + sin(angle) * startR
            ),
            end = Offset(
                wheelCenter.x + cos(angle) * endR,
                wheelCenter.y + sin(angle) * endR
            ),
            strokeWidth = 2f
        )
    }

    // Centre de la roue
    drawCircle(
        color = accent,
        radius = wheelRadius * 0.12f,
        center = wheelCenter
    )

    // Poignée
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.7f), accent)
        ),
        topLeft = Offset(w * 0.68f, h * 0.42f),
        size = Size(w * 0.16f, h * 0.08f),
        cornerRadius = CornerRadius(4f)
    )

    // Halo doré (légèrement entrouvert)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.4f), accent.copy(alpha = 0f))
        ),
        start = Offset(w * 0.08f, h * 0.12f),
        end = Offset(w * 0.08f, h * 0.88f),
        strokeWidth = 6f * glowIntensity
    )
}

// ═══════════════════════════════════
// URNE — 100 Questions
// ═══════════════════════════════════
fun DrawScope.drawUrn(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor
    val centerX = w * 0.5f

    val urnPath = Path().apply {
        moveTo(centerX, h * 0.15f)
        cubicTo(
            centerX + w * 0.28f, h * 0.15f,
            centerX + w * 0.32f, h * 0.35f,
            centerX + w * 0.28f, h * 0.65f
        )
        lineTo(centerX + w * 0.22f, h * 0.80f)
        lineTo(centerX - w * 0.22f, h * 0.80f)
        lineTo(centerX - w * 0.28f, h * 0.65f)
        cubicTo(
            centerX - w * 0.32f, h * 0.35f,
            centerX - w * 0.28f, h * 0.15f,
            centerX, h * 0.15f
        )
        close()
    }

    drawPath(
        path = urnPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A2035),
                Color(0xFF1A1525),
                Color(0xFF2A2035)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, 0f)
        )
    )

    // Motifs dorés sur l'urne (v8.9.8 : Utilise accentColor discret)
    repeat(4) { i ->
        val y = h * (0.30f + i * 0.12f)
        drawLine(
            color = accent.copy(alpha = 0.25f),
            start = Offset(centerX - w * 0.25f, y),
            end = Offset(centerX + w * 0.25f, y),
            strokeWidth = 0.8f)
    }

    // Rouleaux qui dépassent (v8.9.8 : Couleur papier adaptative)
    listOf(-0.12f, 0f, 0.12f).forEach { offsetX ->
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(theme.contentColor.copy(alpha = 0.05f), theme.contentColor.copy(alpha = 0.1f))
            ),
            topLeft = Offset(centerX + w * offsetX - w * 0.04f, h * 0.06f),
            size = Size(w * 0.08f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.04f)
        )
    }

    // Couvercle
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF3A3050), Color(0xFF2A2040))
        ),
        topLeft = Offset(centerX - w * 0.22f, h * 0.10f),
        size = Size(w * 0.44f, h * 0.10f)
    )
}

// ═══════════════════════════════════
// LE PACTE — Deux livres reliés
// ═══════════════════════════════════
fun DrawScope.drawPactBooks(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor

    // Livre A (gauche)
    val bookALeft = w * 0.08f
    val bookATop = h * 0.20f
    val bookW = w * 0.36f
    val bookH = h * 0.60f

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF1A3A5C), Color(0xFF0F2035))
        ),
        topLeft = Offset(bookALeft, bookATop),
        size = Size(bookW, bookH),
        cornerRadius = CornerRadius(4f)
    )

    // Tranche dorée livre A (v8.9.8 : Utilise accentColor)
    drawRect(
        color = accent.copy(alpha = 0.7f),
        topLeft = Offset(bookALeft + bookW - 3f, bookATop),
        size = Size(3f, bookH)
    )

    // Livre B (droite)
    val bookBLeft = w * 0.56f

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF3A1A5C), Color(0xFF200F35))
        ),
        topLeft = Offset(bookBLeft, bookATop + h * 0.06f),
        size = Size(bookW, bookH),
        cornerRadius = CornerRadius(4f)
    )

    // Tranche dorée livre B (v8.9.8 : Utilise accentColor)
    drawRect(
        color = accent.copy(alpha = 0.7f),
        topLeft = Offset(bookBLeft, bookATop + h * 0.06f),
        size = Size(3f, bookH)
    )

    // Ruban doré qui relie les deux livres (v8.9.8 : Utilise accentColor)
    val ribbonPath = Path().apply {
        moveTo(bookALeft + bookW, h * 0.42f)
        cubicTo(
            w * 0.46f, h * 0.35f,
            w * 0.54f, h * 0.65f,
            bookBLeft, h * 0.55f
        )
    }
    drawPath(
        path = ribbonPath,
        color = accent,
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )

    // Nœud central du ruban
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.7f), accent)
        ),
        radius = 6f,
        center = Offset(w * 0.5f, h * 0.49f)
    )
}

// ═══════════════════════════════════
// LETTRE SCELLÉE — Réconciliation
// ═══════════════════════════════════
fun DrawScope.drawSealedLetter(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor

    // Enveloppe fermée (v8.9.8 : Couleur papier adaptative)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(theme.contentColor.copy(alpha = 0.05f), theme.contentColor.copy(alpha = 0.08f))
        ),
        topLeft = Offset(w * 0.10f, h * 0.25f),
        size = Size(w * 0.80f, h * 0.52f),
        cornerRadius = CornerRadius(4f)
    )

    // Rabat fermé
    val rabatPath = Path().apply {
        moveTo(w * 0.10f, h * 0.25f)
        lineTo(w * 0.50f, h * 0.48f)
        lineTo(w * 0.90f, h * 0.25f)
        close()
    }
    drawPath(
        path = rabatPath,
        color = theme.contentColor.copy(alpha = 0.05f)
    )
    drawPath(
        path = rabatPath,
        color = theme.contentColor.copy(alpha = 0.15f),
        style = Stroke(width = 0.5f)
    )

    // Grand cachet de cire (v8.9.8 : Couleur matière conservée)
    val sealCenter = Offset(w * 0.5f, h * 0.48f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF8B1A1A), Color(0xFF5A0A0A)),
            center = sealCenter,
            radius = w * 0.08f
        ),
        radius = w * 0.08f,
        center = sealCenter
    )

    // Motif sur le cachet
    drawCircle(
        color = theme.backgroundColor.copy(alpha = 0.3f),
        radius = w * 0.06f,
        center = sealCenter,
        style = Stroke(width = 1f)
    )

    // Ruban croisé
    drawLine(
        color = accent.copy(alpha = 0.4f),
        start = Offset(w * 0.10f, h * 0.48f),
        end = Offset(w * 0.90f, h * 0.48f),
        strokeWidth = 2f
    )
    drawLine(
        color = accent.copy(alpha = 0.4f),
        start = Offset(w * 0.50f, h * 0.25f),
        end = Offset(w * 0.50f, h * 0.77f),
        strokeWidth = 2f
    )

    // Halo rouge discret
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x20FF4444),
                Color(0x00FF4444)
            ),
            center = sealCenter,
            radius = w * 0.25f * glowIntensity
        ),
        radius = w * 0.25f * glowIntensity,
        center = sealCenter
    )
}
