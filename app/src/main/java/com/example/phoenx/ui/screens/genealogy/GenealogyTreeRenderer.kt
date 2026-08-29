package com.example.phoenx.ui.screens.genealogy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phoenx.domain.model.ResolvedPerson
import com.example.phoenx.domain.model.TreeLayout
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * GenealogyTreeRenderer (v9.4.29)
 * Rendu visuel de l'Arbre avec Zoom et Pan.
 */
@Composable
fun GenealogyTreeRenderer(
    layout: TreeLayout,
    onPersonClick: (ResolvedPerson) -> Unit,
    onAddChild: (ResolvedPerson) -> Unit,
    creatorId: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val density = LocalDensity.current

    // États pour le Zoom et le Pan
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Centrage initial sur le créateur
    LaunchedEffect(layout, creatorId) {
        if (creatorId != null) {
            val creatorNode = layout.nodes.find { it.person.id == creatorId }
            if (creatorNode != null) {
                scale = 1f
                offset = Offset(-creatorNode.x, -creatorNode.y)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.4f, 2.5f)
                        offset += pan / scale
                    }
                }
        ) {
            val totalWidth = this.maxWidth
            val totalHeight = this.maxHeight
            
            val centerX = with(density) { (totalWidth / 2).toPx() }
            val centerY = with(density) { (totalHeight / 2).toPx() }

            val nodeWidth = 140.dp
            val nodeHeight = 180.dp

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
                // 1. DESSIN DES LIENS (CANVAS)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    layout.connections.forEach { (parentIds, childId) ->
                        val parentNodes = layout.nodes.filter { parentIds.contains(it.person.id) }
                        val childNode = layout.nodes.find { it.person.id == childId }

                        if (parentNodes.isNotEmpty() && childNode != null) {
                            val avgParentX = parentNodes.map { it.x }.average().toFloat()
                            val parentY = parentNodes.first().y

                            val start = Offset(avgParentX.dp.toPx() + centerX, parentY.dp.toPx() + centerY + 40.dp.toPx())
                            val end = Offset(childNode.x.dp.toPx() + centerX, childNode.y.dp.toPx() + centerY - 60.dp.toPx())

                            val path = Path().apply {
                                moveTo(start.x, start.y)
                                cubicTo(
                                    start.x, (start.y + end.y) / 2,
                                    end.x, (start.y + end.y) / 2,
                                    end.x, end.y
                                )
                            }
                            drawPath(path = path, color = accent.copy(alpha = 0.2f), style = Stroke(width = 2.dp.toPx()))
                        }
                    }

                    layout.coupleConnections.forEach { (id1, id2) ->
                        val n1 = layout.nodes.find { it.person.id == id1 }
                        val n2 = layout.nodes.find { it.person.id == id2 }
                        if (n1 != null && n2 != null) {
                            val p1 = Offset(n1.x.dp.toPx() + centerX, n1.y.dp.toPx() + centerY)
                            val p2 = Offset(n2.x.dp.toPx() + centerX, n2.y.dp.toPx() + centerY)
                            drawLine(color = accent.copy(alpha = 0.4f), start = p1, end = p2, strokeWidth = 3.dp.toPx())
                        }
                    }
                }

                // 2. DESSIN DES NŒUDS (COMPOSABLES)
                layout.nodes.forEach { node ->
                    val isSpouse = node.person.parentIds.isEmpty() && 
                        layout.coupleConnections.any { it.first == node.person.id || it.second == node.person.id }

                    val xOffset = node.x.dp + (totalWidth / 2) - (nodeWidth / 2)
                    val yOffset = node.y.dp + (totalHeight / 2) - (nodeHeight / 2)

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .width(nodeWidth)
                            .height(nodeHeight)
                            .zIndex(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        PersonNodeCard(
                            person = node.person,
                            onClick = { onPersonClick(node.person) },
                            onAddChild = { onAddChild(node.person) },
                            accent = accent,
                            enabled = enabled,
                            isDotted = isSpouse,
                            creatorId = creatorId
                        )
                    }
                }
            }
        }

        // Bouton flottant de Recentrage
        FloatingActionButton(
            onClick = {
                val creatorNode = layout.nodes.find { it.person.id == creatorId }
                if (creatorNode != null) {
                    scale = 1f
                    offset = Offset(-creatorNode.x, -creatorNode.y)
                } else {
                    scale = 1f
                    offset = Offset.Zero
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = theme.backgroundColor,
            contentColor = accent
        ) {
            Icon(Icons.Default.MyLocation, "Recentrer")
        }
    }
}

@Composable
fun PersonNodeCard(
    person: ResolvedPerson,
    onClick: () -> Unit,
    onAddChild: () -> Unit,
    accent: Color,
    enabled: Boolean = true,
    isDotted: Boolean = false,
    creatorId: String? = null
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val mediaManager = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.example.phoenx.data.media.MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }
    var showReparentInfo by remember { mutableStateOf(false) }

    if (showReparentInfo) {
        AlertDialog(
            onDismissRequest = { showReparentInfo = false },
            confirmButton = { TextButton(onClick = { showReparentInfo = false }) { Text("Compris", color = accent) } },
            title = { Text("Lien automatique", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Cette personne a été automatiquement repositionnée après la suppression d'un intermédiaire dans la lignée.", color = theme.contentColor.copy(alpha = 0.7f)) },
            containerColor = theme.backgroundColor
        )
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            val borderColor = if (person.isDeceased) Color.Gray.copy(alpha = 0.5f) else accent
            
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(theme.contentColor.copy(alpha = 0.05f))
                    .then(
                        if (isDotted) {
                            Modifier.drawWithCache {
                                onDrawBehind {
                                    drawCircle(
                                        color = borderColor,
                                        radius = size.minDimension / 2,
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )
                                }
                            }
                        } else {
                            Modifier.border(2.dp, borderColor, CircleShape)
                        }
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                if (person.photoUrl != null) {
                    val isPathEncrypted = person.photoUrl.endsWith(".enc")
                    com.example.phoenx.ui.components.SecureAsyncImage(
                        mediaUrl = person.photoUrl,
                        mediaManager = mediaManager,
                        creatorId = creatorId,
                        docType = "persons",
                        docId = person.id,
                        field = "imageUrl",
                        isEncrypted = isPathEncrypted,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person, 
                        null, 
                        tint = theme.contentColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (person.isReparented) {
                Box(
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .background(accent, RoundedCornerShape(10.dp))
                        .border(1.dp, theme.backgroundColor, RoundedCornerShape(10.dp))
                        .clickable { showReparentInfo = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!person.reparentedRelationLabel.isNullOrBlank()) {
                        Text(
                            text = person.reparentedRelationLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                            color = theme.backgroundColor
                        )
                    } else {
                        Icon(
                            Icons.Default.Link, 
                            null, 
                            tint = theme.backgroundColor, 
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = person.firstName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (person.isDeceased) theme.contentColor.copy(alpha = 0.5f) else theme.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        if (person.lastName != null) {
            Text(
                text = person.lastName,
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (enabled) {
            IconButton(
                onClick = onAddChild,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(24.dp)
                    .background(accent.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(16.dp))
            }
        }
    }
}
