package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*

/**
 * Composant Cameo (v8.9.9) : Affiche le portrait d'un proche dans un ovale "médaillon"
 * avec un filtre artistique "Portrait au Fusain" (Grayscale + High Contrast).
 * Supporte désormais la résolution de chemin Storage (v9.4.17).
 */
@Composable
fun CameoPortrait(
    imagePath: String?,
    firstName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current

    // Récupération du MediaManager via EntryPoint (v9.4.17)
    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    var displayUrl by remember(imagePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(imagePath) {
        if (!imagePath.isNullOrBlank()) {
            if (java.io.File(imagePath).exists()) {
                displayUrl = imagePath // Chemin local
            } else {
                displayUrl = mediaManager.getSafeUrl(imagePath) // Chemin Storage ou URL
            }
        }
    }

    // Forme Ovale Cameo (plus haut que large, ratio ~1.2)
    val cameoShape = GenericShape { size, _ ->
        addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
    }

    Box(
        modifier = modifier
            .size(width = size, height = size * 1.25f)
            .clip(cameoShape)
            .background(theme.contentColor.copy(alpha = 0.05f))
            .border(1.dp, accent.copy(alpha = 0.3f), cameoShape),
        contentAlignment = Alignment.Center
    ) {
        if (!displayUrl.isNullOrBlank()) {
            // Filtre "Portrait au Fusain" via ColorMatrix
            val charcoalMatrix = ColorMatrix().apply {
                setToSaturation(0f) // Noir et blanc
                // Augmentation du contraste (ajustement manuel des échelles)
                val contrast = 1.2f
                val translate = -0.1f
                this[0, 0] = contrast
                this[1, 1] = contrast
                this[2, 2] = contrast
                this[0, 4] = translate * 255
                this[1, 4] = translate * 255
                this[2, 4] = translate * 255
            }

            AsyncImage(
                model = displayUrl,
                contentDescription = "Portrait de $firstName",
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(charcoalMatrix),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder Initiale style Fusain
            Text(
                text = firstName.take(1).uppercase(),
                color = accent.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4).sp,
                    fontFamily = theme.fontFamily
                )
            )
        }
        
        // Bordure intérieure estompée pour effet médaillon
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, theme.backgroundColor.copy(alpha = 0.2f), cameoShape)
        )
    }
}
