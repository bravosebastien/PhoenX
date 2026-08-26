package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * EncounterGraphRenderer (v9.5.2 - ÉTAPE B1)
 * Rendu avec Zoom, Pan et Cadrage initial.
 * Note : Logique de navigation dupliquée de l'Arbre pour isolation totale.
 */
@Composable
fun EncounterGraphRenderer(
    layout: EncounterLayout,
    onPersonClick: (PersonEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val density = LocalDensity.current

    // États pour le Zoom et le Pan (Indépendants de l'Arbre)
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Cadrage initial sur la première rencontre
    LaunchedEffect(layout) {
        if (layout.nodes.isNotEmpty()) {
            val firstNode = layout.nodes.minByOrNull { it.y }!!
            scale = 1f
            // Centrage horizontal et positionnement en haut avec marge
            offset = Offset(-firstNode.x, -firstNode.y + 100f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.3f, 3f)
                        offset += pan / scale
                    }
                }
        ) {
            val totalWidth = this.maxWidth
            val totalHeight = this.maxHeight
            
            val centerX = with(density) { (totalWidth / 2).toPx() }
            val centerY = with(density) { (totalHeight / 2).toPx() }

            // LE CONTENEUR UNIQUE DE TRANSFORMATION
            // Garantit que tout le contenu reste synchronisé lors du zoom/pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = (offset.x * scale) + centerX - (centerX * scale),
                        translationY = (offset.y * scale) + centerY - (centerY * scale)
                    )
            ) {
                // [Palier B2] Ici viendra le Canvas pour le tracé du chemin central

                layout.nodes.forEach { node ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = node.x.dp - 50.dp, 
                                y = node.y.dp - 30.dp
                            )
                            .size(100.dp, 60.dp)
                            .background(Color.LightGray, RoundedCornerShape(8.dp))
                            .border(1.dp, theme.contentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable { onPersonClick(node.person) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = node.person.firstName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Bouton de Recentrage - Déplacé en bas à gauche (v9.5.2) pour éviter le bouton "+"
        FloatingActionButton(
            onClick = {
                if (layout.nodes.isNotEmpty()) {
                    val firstNode = layout.nodes.minByOrNull { it.y }!!
                    scale = 1f
                    offset = Offset(-firstNode.x, -firstNode.y + 100f)
                } else {
                    scale = 1f
                    offset = Offset.Zero
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            containerColor = theme.backgroundColor,
            contentColor = theme.accentColor,
            shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, "Recentrer")
        }
    }
}
