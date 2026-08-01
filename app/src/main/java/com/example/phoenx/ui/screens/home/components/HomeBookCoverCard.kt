package com.example.phoenx.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.theme.AppThemeState
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun BookCoverCard(
    title: String,
    chaptersCount: Int,
    coverImageUrl: String? = null,
    defaultCoverUrl: String? = null,
    coverTitleStyle: String = "GOLD",
    onClick: () -> Unit,
    theme: AppThemeState,
    // v9.4.19 : Prise en charge du cadrage
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f
) {
    val accent = theme.accentColor
    val context = LocalContext.current
    
    // Récupération du MediaManager via EntryPoint (v9.4.17)
    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    var displayUrl by remember(coverImageUrl, defaultCoverUrl) { mutableStateOf<String?>(null) }
    val finalCoverUrl = coverImageUrl ?: defaultCoverUrl

    LaunchedEffect(finalCoverUrl) {
        displayUrl = mediaManager.getSafeUrl(finalCoverUrl)
    }

    val hasBackgroundImage = displayUrl != null
    
    val titleBrush = getTitleBrush(coverTitleStyle)
    val titleColor = when(coverTitleStyle) {
        "BLACK" -> Color.Black.copy(alpha = 0.85f)
        "WHITE" -> Color.White.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.85f)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            "MON LIVRE",
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
            colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.6f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // FOND : Image personnalisée ou par défaut Remote Config (v9.2.5)
                if (hasBackgroundImage) {
                    coil3.compose.AsyncImage(
                        model = displayUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Voile sombre progressif réduit pour la visibilité du livre (v9.2.7)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                } else {
                    // 3. Stylized Drawing (Fallback v9.2.6)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.copy(alpha = 0.02f), accent.copy(alpha = 0.08f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).alpha(0.05f),
                            tint = accent
                        )
                    }
                }

                // Tranche stylisée (v9.2.1: Renforcée)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(14.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    (if (hasBackgroundImage) Color.Black else accent).copy(alpha = 0.4f),
                                    (if (hasBackgroundImage) Color.Black else accent).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), // v9.2.7 : Top quasi-collé
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    var fontSize by remember(title) { mutableStateOf(20.sp) }
                    
                    val baseTextStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = theme.fontFamily,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic,
                        lineHeight = (fontSize.value * 1.2).sp,
                        shadow = if (hasBackgroundImage || coverTitleStyle != "BLACK") androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.4f),
                            offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                            blurRadius = 4f
                        ) else null
                    )

                    Text(
                        text = title,
                        onTextLayout = { result ->
                            // v9.2.3 : Réduction plus agressive pour tablette et gros réglages police
                            if (result.hasVisualOverflow && fontSize > 10.sp) {
                                fontSize = (fontSize.value - 1).sp
                            }
                        },
                        style = if (titleBrush != null) baseTextStyle.copy(brush = titleBrush) 
                                else baseTextStyle.copy(color = titleColor),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.5.dp)
                            .background((if (hasBackgroundImage) Color.White else accent).copy(alpha = 0.4f))
                    )
                    
                    // Suppression "X chapitres validés" (v9.2.7)
                }
                
                // Effet de relief sur le bord droit
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, accent.copy(alpha = 0.05f))
                            )
                        )
                )
            }
        }
    }
}

fun getTitleBrush(style: String): Brush? {
    val alpha = 0.82f // v9.2.7 : Rendu plus doux
    return when (style) {
        "GOLD" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8860B).copy(alpha = alpha), 
                Color(0xFFFFF8DC).copy(alpha = alpha), 
                Color(0xFFB8860B).copy(alpha = alpha)
            )
        )
        "SILVER" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFA8A9AD).copy(alpha = alpha), 
                Color(0xFFFFFFFF).copy(alpha = alpha), 
                Color(0xFFA8A9AD).copy(alpha = alpha)
            )
        )
        "EMERALD" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF046307).copy(alpha = alpha), 
                Color(0xFF50C878).copy(alpha = alpha), 
                Color(0xFF046307).copy(alpha = alpha)
            )
        )
        "RED" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A0404).copy(alpha = alpha), 
                Color(0xFFB22222).copy(alpha = alpha), 
                Color(0xFF7B0323).copy(alpha = alpha)
            )
        )
        else -> null
    }
}
