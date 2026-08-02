package com.example.phoenx.ui.screens.genealogy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phoenx.domain.model.ResolvedPerson
import com.example.phoenx.domain.model.TreeLayout
import com.example.phoenx.ui.theme.LocalAppTheme

/**
 * GenealogyTreeRenderer (Lot 2 - v9.4.22)
 * Rendu visuel de l'Arbre Généalogique.
 * Canvas pour les liens, Composables pour les nœuds.
 */
@Composable
fun GenealogyTreeRenderer(
    layout: TreeLayout,
    onPersonClick: (ResolvedPerson) -> Unit,
    onAddChild: (ResolvedPerson) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val treeScope = this
        
        Box(modifier = Modifier.size(treeScope.maxWidth, treeScope.maxHeight)) {
            val canvasWidth = treeScope.constraints.maxWidth.toFloat()
            val canvasHeight = treeScope.constraints.maxHeight.toFloat()
            
            val nodeWidth = 140.dp
            val nodeHeight = 160.dp
            
            // 1. DESSIN DES LIENS (CANVAS)
            Canvas(modifier = Modifier.fillMaxSize()) {
                layout.connections.forEach { (parentId, childId) ->
                    val parentNode = layout.nodes.find { it.person.id == parentId }
                    val childNode = layout.nodes.find { it.person.id == childId }
                    
                    if (parentNode != null && childNode != null) {
                        val start = Offset(parentNode.x * canvasWidth, parentNode.y * canvasHeight + 50f)
                        val end = Offset(childNode.x * canvasWidth, childNode.y * canvasHeight - 50f)
                        
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(
                                start.x, (start.y + end.y) / 2,
                                end.x, (start.y + end.y) / 2,
                                end.x, end.y
                            )
                        }
                        
                        drawPath(
                            path = path,
                            color = accent.copy(alpha = 0.3f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // 2. DESSIN DES NŒUDS (COMPOSABLES)
            layout.nodes.forEach { node ->
                val xOffset = (node.x * treeScope.maxWidth.value).dp - (nodeWidth / 2)
                val yOffset = (node.y * treeScope.maxHeight.value).dp - (nodeHeight / 2)

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .width(nodeWidth)
                        .height(nodeHeight),
                    contentAlignment = Alignment.Center
                ) {
                    PersonNodeCard(
                        person = node.person,
                        onClick = { onPersonClick(node.person) },
                        onAddChild = { onAddChild(node.person) },
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
fun PersonNodeCard(
    person: ResolvedPerson,
    onClick: () -> Unit,
    onAddChild: () -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Portrait
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(theme.contentColor.copy(alpha = 0.05f))
                .border(2.dp, if (person.isDeceased) Color.Gray.copy(alpha = 0.5f) else accent, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (person.photoUrl != null) {
                AsyncImage(
                    model = person.photoUrl,
                    contentDescription = null,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Nom
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

        // Bouton "+" pour ajouter un enfant
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
