package com.example.phoenx.ui.screens.library.archive

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.phoenx.ui.screens.library.*
import kotlin.math.*

// Dessine le contenu visuel de chaque compartiment
fun DrawScope.drawCompartmentContent(
    compartment: LibraryCompartment,
    glowIntensity: Float
) {
    when (compartment.id) {
        CompartmentId.BIBLIOTHEQUE -> drawBooks(glowIntensity)
        CompartmentId.DISCOTHEQUE -> drawVinyls(glowIntensity)
        CompartmentId.VIDEOTHEQUE -> drawCassettes(glowIntensity)
        CompartmentId.FIL_PENSEE -> drawScrolls(glowIntensity)
        CompartmentId.LETTRES -> drawMailbox(glowIntensity)
        CompartmentId.MES_MEILLEURS -> drawCuriosityCabinet(glowIntensity)
        CompartmentId.PHOTOS -> drawPhotoFrames(glowIntensity)
        CompartmentId.MAPPEMONDE -> drawGlobe(glowIntensity)
        CompartmentId.CENT_QUESTIONS -> drawUrn(glowIntensity)
        CompartmentId.COFFRE_FORT -> drawSafe(glowIntensity)
        CompartmentId.TIROIR_SECRET -> drawSecretDrawer(glowIntensity)
        CompartmentId.LE_PACTE -> drawPactBooks(glowIntensity)
        CompartmentId.PORTRAIT_PROCHE -> drawPortraitFrame(glowIntensity)
        CompartmentId.RECONCILIATION -> drawSealedLetter(glowIntensity)
    }
}

// ═══════════════════════════════════
// BIBLIOTHÈQUE — Livres sur étagère
// ═══════════════════════════════════
fun DrawScope.drawBooks(glowIntensity: Float) {
    val w = size.width
    val h = size.height
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

        // Tranche dorée (haut du livre)
        drawRect(
            color = Color(0xFFC97B3A).copy(alpha = 0.6f),
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookRight - bookLeft, 3f)
        )

        // Séparation entre livres
        drawLine(
            color = Color(0xFF0D0D10),
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
fun DrawScope.drawVinyls(glowIntensity: Float) {
    val w = size.width
    val h = size.height
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
                color = Color(0xFF2E2E35),
                radius = radius * (r / 10f),
                center = Offset(centerX, centerY),
                style = Stroke(width = 0.8f)
            )
        }

        // Étiquette centrale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC97B3A), Color(0xFF8B5A1E)),
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
                    Color(0x30FFFFFF),
                    Color(0x00FFFFFF)
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
fun DrawScope.drawCassettes(glowIntensity: Float) {
    val w = size.width
    val h = size.height
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
                colors = listOf(Color(0xFFF2EDE8), Color(0xFFE8DFD0))
            ),
            topLeft = Offset(left + casW * 0.08f, top + casH * 0.55f),
            size = Size(casW * 0.84f, casH * 0.35f)
        )

        // Lignes d'étiquette
        repeat(3) { lineIndex ->
            drawLine(
                color = Color(0xFFC97B3A).copy(alpha = 0.4f),
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

// ═══════════════════════════════════
// FIL DE PENSÉE — Parchemins
// ═══════════════════════════════════
fun DrawScope.drawScrolls(glowIntensity: Float) {
    val w = size.width
    val h = size.height

    // Horloge ancienne en arrière-plan
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2A2018), Color(0xFF1C1410))
        ),
        radius = minOf(w, h) * 0.25f,
        center = Offset(w * 0.5f, h * 0.4f)
    )
    drawCircle(
        color = Color(0xFFC97B3A),
        radius = minOf(w, h) * 0.25f,
        center = Offset(w * 0.5f, h * 0.4f),
        style = Stroke(width = 2f)
    )
    // Aiguilles
    drawLine(
        color = Color(0xFFC97B3A),
        start = Offset(w * 0.5f, h * 0.4f),
        end = Offset(w * 0.5f, h * 0.22f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFE8A85F),
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
                colors = listOf(Color(0xFFF5EFE6), Color(0xFFEDE5D8))
            ),
            topLeft = Offset(center.x - pW / 2, center.y - pH / 2),
            size = Size(pW, pH),
            cornerRadius = CornerRadius(pW / 2)
        )
        // Ruban doré
        drawLine(
            color = Color(0xFFC97B3A),
            start = Offset(center.x - pW / 2, center.y),
            end = Offset(center.x + pW / 2, center.y),
            strokeWidth = 1.5f
        )
    }
}

// ═══════════════════════════════════
// BOÎTE AUX LETTRES — Enveloppes
// ═══════════════════════════════════
fun DrawScope.drawMailbox(glowIntensity: Float) {
    val w = size.width
    val h = size.height

    // Meuble à courrier
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF2A1F10), Color(0xFF1C1410))
        ),
        topLeft = Offset(w * 0.1f, h * 0.3f),
        size = Size(w * 0.8f, h * 0.6f),
        cornerRadius = CornerRadius(6f)
    )

    // Compartiments
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

        // Enveloppe
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF5EFE6), Color(0xFFEDE5D8))
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
            color = Color(0xFFEDE5D8)
        )

        // Cachet de cire
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
// CABINET DE CURIOSITÉS — Mes Meilleurs
// ═══════════════════════════════════
fun DrawScope.drawCuriosityCabinet(glowIntensity: Float) {
    val w = size.width
    val h = size.height

    // Vitrine avec verre
    drawRoundRect(
        color = Color(0xFF1C1410),
        topLeft = Offset(w * 0.05f, h * 0.1f),
        size = Size(w * 0.9f, h * 0.8f),
        cornerRadius = CornerRadius(4f)
    )
    // Effet verre
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x15FFFFFF), Color(0x05FFFFFF)
            )
        ),
        topLeft = Offset(w * 0.05f, h * 0.1f),
        size = Size(w * 0.9f, h * 0.8f),
        cornerRadius = CornerRadius(4f)
    )

    // Étagères intérieures
    listOf(0.42f, 0.65f).forEach { y ->
        drawLine(
            color = Color(0xFF2A1F10),
            start = Offset(w * 0.07f, h * y),
            end = Offset(w * 0.93f, h * y),
            strokeWidth = 3f
        )
    }

    // Mini livre
    drawRect(
        color = Color(0xFF8B2020),
        topLeft = Offset(w * 0.12f, h * 0.20f),
        size = Size(w * 0.12f, h * 0.18f)
    )

    // Mini disque
    drawCircle(
        color = Color(0xFF1A1A1F),
        radius = w * 0.08f,
        center = Offset(w * 0.40f, h * 0.30f)
    )
    drawCircle(
        color = Color(0xFFC97B3A),
        radius = w * 0.025f,
        center = Offset(w * 0.40f, h * 0.30f)
    )

    // Mini bobine film
    drawCircle(
        color = Color(0xFF2A2A35),
        radius = w * 0.07f,
        center = Offset(w * 0.72f, h * 0.28f),
        style = Stroke(width = 8f)
    )
}

// ═══════════════════════════════════
// GALERIE — Cadres photos
// ═══════════════════════════════════
fun DrawScope.drawPhotoFrames(glowIntensity: Float) {
    val w = size.width
    val h = size.height

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

        // Cadre doré
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8B5A1E),
                    Color(0xFFC97B3A),
                    Color(0xFF8B5A1E)
                )
            ),
            topLeft = Offset(fLeft, fTop),
            size = Size(fW, fH),
            cornerRadius = CornerRadius(3f)
        )

        // Zone photo (intérieur)
        val border = 6f
        drawRect(
            color = Color(0xFF2E2E35),
            topLeft = Offset(fLeft + border, fTop + border),
            size = Size(fW - border * 2, fH - border * 2)
        )

        // Shimmer de lumière sur le cadre
        drawLine(
            color = Color(0x40E8A85F),
            start = Offset(fLeft, fTop),
            end = Offset(fLeft + fW * 0.3f, fTop + fH * 0.3f),
            strokeWidth = 2f
        )
    }
}

// ═══════════════════════════════════
// MAPPEMONDE — Globe
// ═══════════════════════════════════
fun DrawScope.drawGlobe(glowIntensity: Float) {
    val w = size.width
    val h = size.height
    val centerX = w * 0.5f
    val centerY = h * 0.48f
    val radius = minOf(w, h) * 0.32f

    // Socle en bois
    drawOval(
        color = Color(0xFF2A1F10),
        topLeft = Offset(centerX - radius * 0.6f, centerY + radius * 0.85f),
        size = Size(radius * 1.2f, radius * 0.2f)
    )

    // Globe — océans
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0A1A2A), Color(0xFF051020)),
            center = Offset(centerX - radius * 0.2f, centerY - radius * 0.2f),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY)
    )

    // Continents simplifiés
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

    // Points lumineux (lieux visités)
    listOf(
        Offset(centerX - radius * 0.05f, centerY - radius * 0.15f),
        Offset(centerX + radius * 0.25f, centerY - radius * 0.20f),
        Offset(centerX - radius * 0.45f, centerY + radius * 0.05f)
    ).forEach { point ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFC97B3A),
                    Color(0x00C97B3A)
                ),
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
            colors = listOf(Color(0x25FFFFFF), Color(0x00FFFFFF)),
            center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
            radius = radius * 0.4f
        ),
        radius = radius * 0.4f,
        center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f)
    )

    // Méridien doré
    drawCircle(
        color = Color(0x30C97B3A),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1f)
    )
}

// ═══════════════════════════════════
// URNE — 100 Questions
// ═══════════════════════════════════
fun DrawScope.drawUrn(glowIntensity: Float) {
    val w = size.width
    val h = size.height
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

    // Motifs dorés sur l'urne
    repeat(4) { i ->
        val y = h * (0.30f + i * 0.12f)
        drawLine(
            color = Color(0x40C97B3A),
            start = Offset(centerX - w * 0.25f, y),
            end = Offset(centerX + w * 0.25f, y),
            strokeWidth = 0.8f
        )
    }

    // Rouleaux qui dépassent
    listOf(-0.12f, 0f, 0.12f).forEach { offsetX ->
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF5EFE6), Color(0xFFEDE5D8))
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
// COFFRE-FORT
// ═══════════════════════════════════
fun DrawScope.drawSafe(glowIntensity: Float) {
    val w = size.width
    val h = size.height

    // Corps du coffre
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF2A2A35), Color(0xFF1A1A25))
        ),
        topLeft = Offset(w * 0.08f, h * 0.12f),
        size = Size(w * 0.84f, h * 0.76f),
        cornerRadius = CornerRadius(8f)
    )

    // Rivets aux coins
    listOf(
        Offset(w * 0.15f, h * 0.20f), Offset(w * 0.85f, h * 0.20f),
        Offset(w * 0.15f, h * 0.80f), Offset(w * 0.85f, h * 0.80f)
    ).forEach { rivet ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE8A85F), Color(0xFF8B5A1E)),
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
                Color(0xFF8B5A1E), Color(0xFFC97B3A),
                Color(0xFFE8A85F), Color(0xFFC97B3A), Color(0xFF8B5A1E)
            ),
            center = wheelCenter
        ),
        radius = wheelRadius,
        center = wheelCenter,
        style = Stroke(width = 4f)
    )

    // Encoches de la roue
    repeat(12) { i ->
        val angle = (i * 30f) * (Math.PI / 180f).toFloat()
        val startR = wheelRadius * 0.85f
        val endR = wheelRadius * 0.95f
        drawLine(
            color = Color(0xFFC97B3A),
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
        color = Color(0xFFC97B3A),
        radius = wheelRadius * 0.12f,
        center = wheelCenter
    )

    // Poignée
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8B5A1E), Color(0xFFC97B3A))
        ),
        topLeft = Offset(w * 0.68f, h * 0.42f),
        size = Size(w * 0.16f, h * 0.08f),
        cornerRadius = CornerRadius(4f)
    )

    // Halo doré (légèrement entrouvert)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x00C97B3A), Color(0x40C97B3A), Color(0x00C97B3A))
        ),
        start = Offset(w * 0.08f, h * 0.12f),
        end = Offset(w * 0.08f, h * 0.88f),
        strokeWidth = 6f * glowIntensity
    )
}

// ═══════════════════════════════════
// TIROIR SECRET — Clé Unique
// ═══════════════════════════════════
fun DrawScope.drawSecretDrawer(glowIntensity: Float) {
    val w = size.width
    val h = size.height

    // Tiroir
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF241A0E), Color(0xFF1C1410))
        ),
        topLeft = Offset(w * 0.06f, h * 0.20f),
        size = Size(w * 0.88f, h * 0.60f),
        cornerRadius = CornerRadius(4f)
    )

    // Grain du bois
    repeat(5) { i ->
        drawLine(
            color = Color(0x15C97B3A),
            start = Offset(w * 0.06f, h * (0.28f + i * 0.10f)),
            end = Offset(w * 0.94f, h * (0.28f + i * 0.10f)),
            strokeWidth = 0.5f
        )
    }

    // Serrure ronde centrale
    val lockCenter = Offset(w * 0.5f, h * 0.5f)
    val lockRadius = minOf(w, h) * 0.12f

    // Halo de la serrure
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x30C97B3A),
                Color(0x00C97B3A)
            ),
            center = lockCenter,
            radius = lockRadius * 2.5f * glowIntensity
        ),
        radius = lockRadius * 2.5f * glowIntensity,
        center = lockCenter
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3A3A45), Color(0xFF2A2A35)),
            center = lockCenter,
            radius = lockRadius
        ),
        radius = lockRadius,
        center = lockCenter
    )
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFF8B5A1E), Color(0xFFC97B3A), Color(0xFF8B5A1E)
            ),
            center = lockCenter
        ),
        radius = lockRadius,
        center = lockCenter,
        style = Stroke(width = 3f)
    )

    // Trou de serrure
    drawCircle(
        color = Color(0xFF0D0D10),
        radius = lockRadius * 0.25f,
        center = lockCenter
    )
    drawRect(
        color = Color(0xFF0D0D10),
        topLeft = Offset(lockCenter.x - lockRadius * 0.12f, lockCenter.y),
        size = Size(lockRadius * 0.24f, lockRadius * 0.35f)
    )
}

// ═══════════════════════════════════
// LE PACTE — Deux livres reliés
// ═══════════════════════════════════
fun DrawScope.drawPactBooks(glowIntensity: Float) {
    val w = size.width
    val h = size.height

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

    // Tranche dorée livre A
    drawRect(
        color = Color(0xFFC97B3A),
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

    // Tranche dorée livre B
    drawRect(
        color = Color(0xFFC97B3A),
        topLeft = Offset(bookBLeft, bookATop + h * 0.06f),
        size = Size(3f, bookH)
    )

    // Ruban doré qui relie les deux livres
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
        color = Color(0xFFC97B3A),
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )

    // Nœud central du ruban
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE8A85F), Color(0xFFC97B3A))
        ),
        radius = 6f,
        center = Offset(w * 0.5f, h * 0.49f)
    )
}

// ═══════════════════════════════════
// PORTRAIT D'UN PROCHE
// ═══════════════════════════════════
fun DrawScope.drawPortraitFrame(glowIntensity: Float) {
    val w = size.width
    val h = size.height

    // Grand cadre portrait
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B5A1E), Color(0xFFC97B3A),
                Color(0xFFE8A85F), Color(0xFFC97B3A), Color(0xFF8B5A1E)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, 0f)
        ),
        topLeft = Offset(w * 0.10f, h * 0.08f),
        size = Size(w * 0.80f, h * 0.84f),
        cornerRadius = CornerRadius(6f)
    )

    // Zone portrait (intérieur)
    val innerPadding = 10f
    drawRoundRect(
        color = Color(0xFF2E2E35),
        topLeft = Offset(w * 0.10f + innerPadding, h * 0.08f + innerPadding),
        size = Size(w * 0.80f - innerPadding * 2, h * 0.84f - innerPadding * 2),
        cornerRadius = CornerRadius(3f)
    )

    // Silhouette humaine stylisée
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

    // Ornements du cadre
    listOf(
        Offset(w * 0.10f, h * 0.08f),
        Offset(w * 0.90f, h * 0.08f),
        Offset(w * 0.10f, h * 0.92f),
        Offset(w * 0.90f, h * 0.92f)
    ).forEach { corner ->
        drawCircle(
            color = Color(0xFFE8A85F),
            radius = 5f,
            center = corner
        )
    }
}

// ═══════════════════════════════════
// LETTRE SCELLÉE — Réconciliation
// ═══════════════════════════════════
fun DrawScope.drawSealedLetter(glowIntensity: Float) {
    val w = size.width
    val h = size.height

    // Enveloppe fermée
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF5EFE6), Color(0xFFEDE5D8))
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
        color = Color(0xFFEDE5D8)
    )
    drawPath(
        path = rabatPath,
        color = Color(0x20000000),
        style = Stroke(width = 0.5f)
    )

    // Grand cachet de cire
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
        color = Color(0xFFE8A85F),
        radius = w * 0.06f,
        center = sealCenter,
        style = Stroke(width = 1f)
    )

    // Ruban croisé
    drawLine(
        color = Color(0xFFC97B3A).copy(alpha = 0.6f),
        start = Offset(w * 0.10f, h * 0.48f),
        end = Offset(w * 0.90f, h * 0.48f),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0xFFC97B3A).copy(alpha = 0.6f),
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
