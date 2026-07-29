package com.example.phoenx.ui.screens.home.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.example.phoenx.ui.theme.AppThemeState

@Composable
fun AnimatedEarthCard(
    textureUrl: String?,
    onClick: () -> Unit,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    val context = LocalContext.current
    var earthBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Chargement manuel unique du Bitmap (v9.2.9)
    LaunchedEffect(textureUrl) {
        if (textureUrl != null) {
            try {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(textureUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                
                val result = loader.execute(request)
                if (result is coil3.request.SuccessResult) {
                    // Coil 3: Extraction du bitmap depuis l'objet Image
                    val bitmap = (result.image as? coil3.BitmapImage)?.bitmap
                    earthBitmap = bitmap?.asImageBitmap()
                    android.util.Log.d("EarthDebug", "Manual bitmap load successful: ${earthBitmap != null}")
                }
            } catch (e: Exception) {
                android.util.Log.e("EarthDebug", "Manual bitmap load failed: ${e.message}")
            }
        }
    }

    // Animation de rotation (Amplitude calée sur 300dp)
    val infiniteTransition = rememberInfiniteTransition(label = "earthRotation")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f, 
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ombre portée circulaire
        Box(
            modifier = Modifier
                .size(130.dp)
                .shadow(20.dp, CircleShape, spotColor = accent.copy(alpha = 0.3f))
        )

        // Sphère
        Card(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(0.5.dp, accent.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                // Fond Marin
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        0.0f to Color(0xFF001D3D),
                        1.0f to Color(0xFF000814)
                    )
                ))

                if (earthBitmap != null) {
                    // Dessin direct au Canvas pour éliminer les rechargements Coil (v9.2.9)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val textureWidthPx = 300.dp.toPx()
                        val currentOffsetPx = offset.dp.toPx()
                        
                        // Instance 1
                        drawImage(
                            image = earthBitmap!!,
                            dstOffset = androidx.compose.ui.unit.IntOffset(currentOffsetPx.toInt(), 0),
                            dstSize = androidx.compose.ui.unit.IntSize(textureWidthPx.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.tint(accent.copy(alpha = 0.6f), BlendMode.SrcAtop)
                        )
                        // Instance 2 (Boucle infinie)
                        drawImage(
                            image = earthBitmap!!,
                            dstOffset = androidx.compose.ui.unit.IntOffset((currentOffsetPx + textureWidthPx).toInt(), 0),
                            dstSize = androidx.compose.ui.unit.IntSize(textureWidthPx.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.tint(accent.copy(alpha = 0.6f), BlendMode.SrcAtop)
                        )
                    }
                } else {
                    // Fallback procédural si Bitmap non prêt
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(400.dp)
                            .offset(x = (offset * (200f/300f)).dp)
                    ) {
                        EarthTexture()
                        EarthTexture()
                    }
                }

                // Effet de relief et Halo (Superposés)
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.12f),
                        0.4f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.8f),
                        center = androidx.compose.ui.geometry.Offset(90f, 90f)
                    )
                ))
                
                Box(modifier = Modifier.fillMaxSize().border(
                    width = 1.2.dp,
                    brush = Brush.radialGradient(
                        0.0f to accent.copy(alpha = 0.5f),
                        1.0f to Color.Transparent
                    ),
                    shape = CircleShape
                ))
            }
        }
    }
}

@Composable
fun EarthTexture() {
    // Dégradé pour les continents (Relief suggéré)
    val continentBrush = Brush.verticalGradient(
        listOf(Color(0xFF52796F), Color(0xFF84A98C), Color(0xFF354F52))
    )
    
    Canvas(modifier = Modifier.width(200.dp).fillMaxHeight()) {
        // AMÉRIQUE DU NORD
        val northAmerica = androidx.compose.ui.graphics.Path().apply {
            moveTo(10.dp.toPx(), 20.dp.toPx())
            lineTo(45.dp.toPx(), 15.dp.toPx())
            lineTo(55.dp.toPx(), 35.dp.toPx())
            lineTo(35.dp.toPx(), 45.dp.toPx())
            lineTo(25.dp.toPx(), 40.dp.toPx())
            close()
        }
        drawPath(northAmerica, continentBrush)

        // AMÉRIQUE DU SUD
        val southAmerica = androidx.compose.ui.graphics.Path().apply {
            moveTo(35.dp.toPx(), 48.dp.toPx())
            lineTo(50.dp.toPx(), 55.dp.toPx())
            lineTo(40.dp.toPx(), 85.dp.toPx())
            lineTo(30.dp.toPx(), 65.dp.toPx())
            close()
        }
        drawPath(southAmerica, continentBrush)

        // EURASIE
        val eurasia = androidx.compose.ui.graphics.Path().apply {
            moveTo(95.dp.toPx(), 15.dp.toPx())
            lineTo(165.dp.toPx(), 10.dp.toPx())
            lineTo(180.dp.toPx(), 30.dp.toPx())
            lineTo(150.dp.toPx(), 45.dp.toPx())
            lineTo(110.dp.toPx(), 35.dp.toPx())
            close()
        }
        drawPath(eurasia, continentBrush)

        // AFRIQUE
        val africa = androidx.compose.ui.graphics.Path().apply {
            moveTo(105.dp.toPx(), 38.dp.toPx())
            lineTo(135.dp.toPx(), 42.dp.toPx())
            lineTo(130.dp.toPx(), 65.dp.toPx())
            lineTo(115.dp.toPx(), 75.dp.toPx())
            lineTo(100.dp.toPx(), 55.dp.toPx())
            close()
        }
        drawPath(africa, continentBrush)

        // AUSTRALIE
        val australia = androidx.compose.ui.graphics.Path().apply {
            moveTo(160.dp.toPx(), 65.dp.toPx())
            lineTo(185.dp.toPx(), 68.dp.toPx())
            lineTo(180.dp.toPx(), 82.dp.toPx())
            lineTo(155.dp.toPx(), 78.dp.toPx())
            close()
        }
        drawPath(australia, continentBrush)

        // QUELQUES ÎLES (Groenland, Japon, UK)
        drawCircle(continentBrush, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(45.dp.toPx(), 10.dp.toPx()))
        drawCircle(continentBrush, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(175.dp.toPx(), 22.dp.toPx()))
        drawCircle(continentBrush, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(105.dp.toPx(), 18.dp.toPx()))
    }
}
