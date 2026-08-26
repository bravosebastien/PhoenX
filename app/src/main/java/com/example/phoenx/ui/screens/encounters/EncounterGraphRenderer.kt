package com.example.phoenx.ui.screens.encounters

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.ui.theme.LocalAppTheme
import kotlin.math.sin

/**
 * EncounterGraphRenderer (v9.5.4 - ÉTAPE B2 CORRECTIF)
 * Alignement strict du Canvas et des Composables.
 */
@Composable
fun EncounterGraphRenderer(
    layout: EncounterLayout,
    onPersonClick: (PersonEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val density = LocalDensity.current
    val accent = theme.accentColor

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(layout) {
        if (layout.nodes.isNotEmpty()) {
            val firstNode = layout.nodes.minByOrNull { it.y }!!
            scale = 1f
            // Centrage horizontal et positionnement du premier noeud en haut
            offset = Offset(-firstNode.x, -firstNode.y + 100f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.2f, 3f)
                        offset += pan / scale
                    }
                }
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val totalHeightPx = constraints.maxHeight.toFloat()
            val centerX = totalWidthPx / 2f
            val centerY = totalHeightPx / 2f

            // LE CONTENEUR UNIQUE DE TRANSFORMATION (Garant d'alignement)
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
                // 1. LE CANVAS (Même taille, même origine que les rectangles)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // A. Le Chemin Central Sinueux (Tracé du haut au bas du contenu)
                    val path = Path()
                    val totalContentHeightPx = layout.totalHeightDp.dp.toPx()
                    val steps = 150
                    
                    for (i in 0..steps) {
                        val yPx = i * (totalContentHeightPx / steps)
                        val yDp = yPx.toDp().value
                        val xDp = EncounterGraphAlgorithm.getPathX(yDp)
                        val xPx = xDp.dp.toPx()
                        
                        if (i == 0) path.moveTo(xPx + centerX, yPx)
                        else path.lineTo(xPx + centerX, yPx)
                    }
                    
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.25f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // B. Liens de liaison (Trait Plein)
                    layout.nodes.forEach { node ->
                        // Position du centre du rectangle en pixels
                        val nodeX = node.x.dp.toPx() + centerX
                        val nodeY = node.y.dp.toPx()
                        val end = Offset(nodeX, nodeY)
                        
                        if (node.parentId == null) {
                            // Rattachement au chemin central
                            val startXPx = EncounterGraphAlgorithm.getPathX(node.y).dp.toPx() + centerX
                            val start = Offset(startXPx, nodeY)
                            drawLine(color = accent.copy(alpha = 0.3f), start = start, end = end, strokeWidth = 2.dp.toPx())
                        } else {
                            // Branche depuis le présentateur
                            val parent = layout.nodes.find { it.person.id == node.parentId }
                            if (parent != null) {
                                val start = Offset(parent.x.dp.toPx() + centerX, parent.y.dp.toPx())
                                drawBezierCurve(this, start, end, accent.copy(alpha = 0.4f))
                            }
                        }

                        // LOG PHOENX_GRAPH de contrôle d'alignement
                        // android.util.Log.e("PHOENX_GRAPH", "Tracé Node ${node.person.firstName} at ($nodeX, $nodeY) PX")
                    }
                }

                // 2. LES MÉDAILLONS (COMPOSABLES)
                layout.nodes.forEach { node ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(density) { (node.x.dp.toPx() + centerX).toDp() - 50.dp }, 
                                y = with(density) { node.y.dp.toPx().toDp() - 30.dp }
                            )
                            .size(100.dp, 60.dp)
                            .background(if (node.isFamily) Color.Gray.copy(alpha = 0.1f) else Color.LightGray, RoundedCornerShape(8.dp))
                            .border(1.dp, if (node.isFamily) accent.copy(alpha = 0.3f) else theme.contentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable { onPersonClick(node.person) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = node.person.firstName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Bouton de Recentrage
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

fun drawBezierCurve(drawScope: androidx.compose.ui.graphics.drawscope.DrawScope, start: Offset, end: Offset, color: Color) {
    with(drawScope) {
        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                start.x, (start.y + end.y) / 2f,
                end.x, (start.y + end.y) / 2f,
                end.x, end.y
            )
        }
        drawPath(
            path = path, 
            color = color, 
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
