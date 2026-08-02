package com.example.phoenx.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phoenx.ui.theme.AppThemeState

/**
 * GenealogyCard (v9.4.22)
 * Carte visuelle pour l'Arbre Généalogique sur l'accueil.
 */
@Composable
fun GenealogyCard(
    imageUrl: String?,
    onClick: () -> Unit,
    theme: AppThemeState
) {
    val accent = theme.accentColor

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .width(160.dp) // Légèrement plus petit que le livre pour tenir à côté si besoin
    ) {
        Text(
            "MA GÉNÉALOGIE",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )
        
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = accent.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), // Fond BLANC par défaut
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Scrim pour la lisibilité du texte
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                )
                            )
                    )
                } else {
                    // Fallback visuel : Icône discrète
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AccountTree,
                            null,
                            modifier = Modifier.size(48.dp).alpha(0.1f),
                            tint = Color.Black
                        )
                    }
                }

                // Titre PAR-DESSUS (toujours visible)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "Mon Arbre\nGénéalogique",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            color = if (imageUrl.isNullOrBlank()) Color.Black.copy(alpha = 0.7f) else Color.White,
                            shadow = if (!imageUrl.isNullOrBlank()) androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                blurRadius = 3f
                            ) else null
                        ),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
