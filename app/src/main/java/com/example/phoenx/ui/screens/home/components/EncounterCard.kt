package com.example.phoenx.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun EncounterCard(
    imageUrl: String?,
    onClick: () -> Unit,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    val accent = theme.accentColor

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            "LES RENCONTRES",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )
        
        Card(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(180.dp)
                .aspectRatio(0.72f)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = accent.copy(alpha = 0.5f),
                    ambientColor = accent.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.6f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // v12.2 : Suppression du scrim pour couleurs naturelles
                } else {
                    // Fallback visuel : Icône discrète
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Handshake,
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
                        text = "Les\nRencontres",
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
