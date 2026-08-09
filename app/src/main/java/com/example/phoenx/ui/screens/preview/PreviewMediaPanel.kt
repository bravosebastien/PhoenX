package com.example.phoenx.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.screens.media.AudioPlayer
import com.example.phoenx.ui.screens.media.VideoPlayer
import com.example.phoenx.ui.screens.media.ZoomableImage
import com.example.phoenx.ui.theme.LocalAppTheme

@UnstableApi
@Composable
fun PreviewMediaPanel(
    entry: OfflineEntry,
    mediaManager: MediaManager,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f) // Occupation ~65% de la hauteur
            .background(theme.backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barre de titre
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.aiSummary.ifBlank { "Aperçu du média" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Text(
                    text = "Vision destinataire (Lecture seule)",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Conteneur Média (Style Cinema)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center
        ) {
            when (entry.entryType) {
                "PHOTO", "GALLERY" -> {
                    ZoomableImage(
                        mediaUrl = entry.mediaUrl,
                        localPath = entry.localMediaPath,
                        explicitKey = null, // Le créateur voit son propre contenu
                        mediaManager = mediaManager
                    )
                }
                "VIDEO" -> {
                    VideoPlayer(
                        mediaUrl = entry.mediaUrl,
                        localPath = entry.localMediaPath,
                        explicitKey = null,
                        mediaManager = mediaManager
                    )
                }
                "AUDIO" -> {
                    AudioPlayer(
                        mediaUrl = entry.mediaUrl,
                        localPath = entry.localMediaPath,
                        explicitKey = null,
                        mediaManager = mediaManager,
                        title = entry.aiSummary
                    )
                }
                else -> {
                    Text("Format non supporté en aperçu", color = Color.White.copy(alpha = 0.4f))
                }
            }
        }
        
        // Commentaire si présent
        if (!entry.userComment.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                color = theme.contentColor.copy(alpha = 0.03f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.userComment!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(12.dp),
                    maxLines = 3
                )
            }
        }
    }
}
