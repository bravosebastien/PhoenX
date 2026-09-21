package com.example.phoenx.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun CompartmentMediaIcon(
    mediaUrl: String?,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    iconSize: Dp = 32.dp,
    containerColor: Color = LocalAppTheme.current.contentColor.copy(alpha = 0.04f),
    iconTint: Color = LocalAppTheme.current.accentColor,
    borderColor: Color = LocalAppTheme.current.contentColor.copy(alpha = 0.1f),
    contentDescription: String? = null
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val hasUrl = !mediaUrl.isNullOrBlank()

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1. Icône statique de repli en fond (toujours présente pendant le chargement)
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )

            // 2. Média personnalisé (photo fixe ou WebP/GIF animé en boucle)
            if (hasUrl) {
                val imageRequest = remember(mediaUrl, context) {
                    ImageRequest.Builder(context)
                        .data(mediaUrl)
                        .crossfade(true)
                        .decoderFactory { result, options, imageLoader ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                AnimatedImageDecoder.Factory().create(result, options, imageLoader)
                            } else {
                                GifDecoder.Factory().create(result, options, imageLoader)
                            }
                        }
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
