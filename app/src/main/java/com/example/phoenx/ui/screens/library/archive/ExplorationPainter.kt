package com.example.phoenx.ui.screens.library.archive

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.phoenx.ui.theme.AppThemeState
import kotlin.math.*

/**
 * ExplorationPainter — Dessine les objets liés au temps, à l'espace et aux visages.
 * v8.9.8 : Intégration du thémage dynamique.
 */

// ═══════════════════════════════════
// FIL DE PENSÉE — Parchemins
// ═══════════════════════════════════
fun DrawScope.drawScrolls(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor

    // Horloge ancienne en arrière-plan
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2A2018), Color(0xFF1C1410))
        ),
        radius = minOf(w, h) * 0.25f,
        center = Offset(w * 0.5f, h * 0.4f)
    )
    drawCircle(
        color = accent,
        radius = minOf(w, h) * 0.25f,
        center = Offset(w * 0.5f, h * 0.4f),
        style = Stroke(width = 2f)
    )
    // Aiguilles (v8.9.8 : AccentColor)
    drawLine(
        color = accent,
        start = Offset(w * 0.5f, h * 0.4f),
        end = Offset(w * 0.5f, h * 0.22f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = accent.copy(alpha = 0.8f),
        start = Offset(w * 0.5f, h * 0.4f),
        end = Offset(w * 0.62f, h * 0.4f),
        strokeWidth = 1.5f,
        cap = StrokeCap.Round
    )

    // Parchemins enroulés
    listOf(
        Offset(w * 0.15f, h * 0.65f),
        Offset(w * 0.42f, h * 0.70f),
        Offset(w * 0.68f, h * 0.63f),
        Offset(w * 0.85f, h * 0.68f)
    ).forEach { center ->
        val pW = w * 0.12f
        val pH = h * 0.22f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(theme.contentColor.copy(alpha = 0.05f), theme.contentColor.copy(alpha = 0.08f))
            ),
            topLeft = Offset(center.x - pW / 2, center.y - pH / 2),
            size = Size(pW, pH),
            cornerRadius = CornerRadius(pW / 2)
        )
        // Ruban doré (v8.9.8 : AccentColor)
        drawLine(
            color = accent,
            start = Offset(center.x - pW / 2, center.y),
            end = Offset(center.x + pW / 2, center.y),
            strokeWidth = 1.5f
        )
    }
}

// ═══════════════════════════════════
// BOÎTE AUX LETTRES — Enveloppes
// ═══════════════════════════════════
fun DrawScope.drawMailbox(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height

    // Meuble à courrier (Bois sombre conservé)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF2A1F10), Color(0xFF1C1410))
        ),
        topLeft = Offset(w * 0.1f, h * 0.3f),
        size = Size(w * 0.8f, h * 0.6f),
        cornerRadius = CornerRadius(6f)
    )

    // Compartiments (v8.9.8 : Structure sombre conservée)
    repeat(3) { row ->
        repeat(2) { col ->
            val left = w * (0.14f + col * 0.42f)
            val top = h * (0.35f + row * 0.18f)
            drawRoundRect(
                color = Color(0xFF151210),
                topLeft = Offset(left, top),
                size = Size(w * 0.36f, h * 0.14f),
                cornerRadius = CornerRadius(3f)
            )
        }
    }

    // Enveloppes qui dépassent
    listOf(0.25f, 0.55f, 0.75f).forEachIndexed { index, xPos ->
        val hasWaxSeal = index == 1

        // Enveloppe (Couleur papier adaptative)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(theme.contentColor.copy(alpha = 0.05f), theme.contentColor.copy(alpha = 0.08f))
            ),
            topLeft = Offset(w * xPos, h * 0.18f),
            size = Size(w * 0.18f, h * 0.20f),
            cornerRadius = CornerRadius(2f)
        )

        // Rabat de l'enveloppe
        val path = Path().apply {
            moveTo(w * xPos, h * 0.18f)
            lineTo(w * (xPos + 0.09f), h * 0.26f)
            lineTo(w * (xPos + 0.18f), h * 0.18f)
            close()
        }
        drawPath(
            path = path,
            color = theme.contentColor.copy(alpha = 0.08f)
        )

        // Cachet de cire (v8.9.8 : Couleur matière conservée)
        if (hasWaxSeal) {
            drawCircle(
                color = Color(0xFF8B1A1A),
                radius = w * 0.025f,
                center = Offset(w * (xPos + 0.09f), h * 0.26f)
            )
        }
    }
}

// ═══════════════════════════════════
// GALERIE — Cadres photos
// ═══════════════════════════════════
fun DrawScope.drawPhotoFrames(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor

    data class Frame(
        val left: Float, val top: Float,
        val width: Float, val height: Float,
        val rotation: Float
    )

    val frames = listOf(
        Frame(0.08f, 0.12f, 0.35f, 0.40f, -3f),
        Frame(0.50f, 0.08f, 0.40f, 0.30f, 2f),
        Frame(0.12f, 0.55f, 0.30f, 0.35f, 1f),
        Frame(0.48f, 0.45f, 0.45f, 0.45f, -1f)
    )

    frames.forEach { frame ->
        val fLeft = w * frame.left
        val fTop = h * frame.top
        val fW = w * frame.width
        val fH = h * frame.height

        // Cadre doré (v8.9.8 : AccentColor Gradient)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    accent.copy(alpha = 0.7f),
                    accent,
                    accent.copy(alpha = 0.7f)
                )
            ),
            topLeft = Offset(fLeft, fTop),
            size = Size(fW, fH),
            cornerRadius = CornerRadius(3f)
        )

        // Zone photo (intérieur sombre fixe)
        val border = 6f
        drawRect(
            color = Color(0xFF2E2E35),
            topLeft = Offset(fLeft + border, fTop + border),
            size = Size(fW - border * 2, fH - border * 2)
        )

        // Shimmer de lumière sur le cadre (v8.9.8 : AccentColor)
        drawLine(
            color = accent.copy(alpha = 0.3f),
            start = Offset(fLeft, fTop),
            end = Offset(fLeft + fW * 0.3f, fTop + fH * 0.3f),
            strokeWidth = 2f
        )
    }
}

// ═══════════════════════════════════
// MAPPEMONDE — Globe
// ═══════════════════════════════════
fun DrawScope.drawGlobe(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor
    val centerX = w * 0.5f
    val centerY = h * 0.48f
    val radius = minOf(w, h) * 0.32f

    // Socle en bois (Couleur matière conservée)
    drawOval(
        color = Color(0xFF2A1F10),
        topLeft = Offset(centerX - radius * 0.6f, centerY + radius * 0.85f),
        size = Size(radius * 1.2f, radius * 0.2f)
    )

    // Globe — océans (Couleur matière conservée)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0A1A2A), Color(0xFF051020)),
            center = Offset(centerX - radius * 0.2f, centerY - radius * 0.2f),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY)
    )

    // Continents simplifiés (Couleur matière conservée)
    val continentColor = Color(0xFF1C3A1C)
    // Europe/Afrique
    drawOval(
        color = continentColor,
        topLeft = Offset(centerX - radius * 0.1f, centerY - radius * 0.4f),
        size = Size(radius * 0.5f, radius * 0.8f)
    )
    // Amériques
    drawOval(
        color = continentColor,
        topLeft = Offset(centerX - radius * 0.7f, centerY - radius * 0.3f),
        size = Size(radius * 0.4f, radius * 0.6f)
    )
    // Asie
    drawOval(
        color = continentColor,
        topLeft = Offset(centerX + radius * 0.1f, centerY - radius * 0.35f),
        size = Size(radius * 0.55f, radius * 0.5f)
    )

    // Points lumineux (lieux visités) (v8.9.8 : AccentColor)
    listOf(
        Offset(centerX - radius * 0.05f, centerY - radius * 0.15f),
        Offset(centerX + radius * 0.25f, centerY - radius * 0.20f),
        Offset(centerX - radius * 0.45f, centerY + radius * 0.05f)
    ).forEach { point ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent, Color.Transparent),
                center = point,
                radius = 12f * glowIntensity
            ),
            radius = 5f,
            center = point
        )
    }

    // Reflet sur le globe
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
            center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
            radius = radius * 0.4f
        ),
        radius = radius * 0.4f,
        center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f)
    )

    // Méridien doré (v8.9.8 : AccentColor discret)
    drawCircle(
        color = accent.copy(alpha = 0.2f),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1f)
    )
}

// ═══════════════════════════════════
// PORTRAIT D'UN PROCHE
// ═══════════════════════════════════
fun DrawScope.drawPortraitFrame(glowIntensity: Float, theme: AppThemeState) {
    val w = size.width
    val h = size.height
    val accent = theme.accentColor

    // Grand cadre portrait (v8.9.8 : AccentColor Gradient)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.7f), accent,
                accent.copy(alpha = 0.8f), accent, accent.copy(alpha = 0.7f)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, 0f)
        ),
        topLeft = Offset(w * 0.10f, h * 0.08f),
        size = Size(w * 0.80f, h * 0.84f),
        cornerRadius = CornerRadius(6f)
    )

    // Zone portrait (intérieur sombre fixe)
    val innerPadding = 10f
    drawRoundRect(
        color = Color(0xFF2E2E35),
        topLeft = Offset(w * 0.10f + innerPadding, h * 0.08f + innerPadding),
        size = Size(w * 0.80f - innerPadding * 2, h * 0.84f - innerPadding * 2),
        cornerRadius = CornerRadius(3f)
    )

    // Silhouette humaine stylisée (Couleur matière conservée)
    val cx = w * 0.5f
    drawCircle(
        color = Color(0xFF3A3A45),
        radius = w * 0.12f,
        center = Offset(cx, h * 0.38f)
    )
    val bodyPath = Path().apply {
        moveTo(cx - w * 0.15f, h * 0.85f)
        cubicTo(cx - w * 0.15f, h * 0.58f, cx + w * 0.15f, h * 0.58f, cx + w * 0.15f, h * 0.85f)
        close()
    }
    drawPath(path = bodyPath, color = Color(0xFF3A3A45))

    // Ornements du cadre (v8.9.8 : AccentColor)
    listOf(
        Offset(w * 0.10f, h * 0.08f),
        Offset(w * 0.90f, h * 0.08f),
        Offset(w * 0.10f, h * 0.92f),
        Offset(w * 0.90f, h * 0.92f)
    ).forEach { corner ->
        drawCircle(
            color = accent.copy(alpha = 0.8f),
            radius = 5f,
            center = corner
        )
    }
}
