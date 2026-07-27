package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme

enum class AvatarShape { CIRCLE, CAMEO }

@Composable
fun PhoenXAvatar(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: AvatarShape = AvatarShape.CIRCLE,
    borderColor: Color? = null
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val avatarShape: Shape = when (shape) {
        AvatarShape.CIRCLE -> CircleShape
        AvatarShape.CAMEO -> GenericShape { size, _ ->
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
    }

    val finalModifier = if (shape == AvatarShape.CAMEO) {
        modifier.size(width = size, height = size * 1.25f)
    } else {
        modifier.size(size)
    }

    Box(
        modifier = finalModifier
            .clip(avatarShape)
            .background(theme.contentColor.copy(alpha = 0.05f))
            .border(1.dp, borderColor ?: accent.copy(alpha = 0.3f), avatarShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Photo de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Outlined.Person, 
                contentDescription = null, 
                tint = accent, 
                modifier = Modifier.size(size * 0.6f)
            )
        }
        
        // Bordure intérieure pour effet de profondeur si Cameo
        if (shape == AvatarShape.CAMEO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, theme.backgroundColor.copy(alpha = 0.2f), avatarShape)
            )
        }
    }
}
