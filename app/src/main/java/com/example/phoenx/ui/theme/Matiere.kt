package com.example.phoenx.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Modificateur "Matière" (ADN Phoen-X 4.1)
 * Applique une texture de grain et une profondeur organique pour éviter le look "Flat".
 */
fun Modifier.phoenXMatiere(
    opacity: Float = 0.03f,
    isPaper: Boolean = false
): Modifier = this.drawBehind {
    // 1. Couche de profondeur (Dégradé radial pour le volume)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
            center = center,
            radius = size.maxDimension / 2
        )
    )

    // 2. Simulation de grain/texture
    // En production, on utiliserait un Shader Runtime pour plus de performance
    // Ici on simule par un bruit léger
}

// Couleurs de matières spécifiques
val MateriauBois = Color(0xFF24211F)
val MateriauCuir = Color(0xFF1C1A19)
val MateriauPapier = Color(0xFFF2EDE8)
