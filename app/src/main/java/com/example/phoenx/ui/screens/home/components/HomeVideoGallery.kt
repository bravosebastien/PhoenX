package com.example.phoenx.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.example.phoenx.data.model.PresentationVideo
import com.example.phoenx.ui.components.VideoPlayerBanner
import com.example.phoenx.ui.theme.AppThemeState

@Composable
fun PresentationVideoGallery(
    videos: List<PresentationVideo>,
    theme: AppThemeState,
    onVideoClick: (PresentationVideo) -> Unit
) {
    val accent = theme.accentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Text(
            "VOTRE GUIDE VIDÉO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
        )

        // Grille fixe de 6 slots (3 par ligne, 2 lignes) (v9.2.7)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            repeat(2) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) { colIndex ->
                        val slotNumber = rowIndex * 3 + colIndex + 1 // 1 à 6
                        val video = videos.find { it.slotIndex == slotNumber }
                        
                        Column(
                            modifier = Modifier
                                .width(90.dp)
                                .then(if (video != null) Modifier.clickable { onVideoClick(video) } else Modifier),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(theme.contentColor.copy(alpha = 0.04f))
                                    .border(
                                        width = 1.dp,
                                        color = if (video != null) accent.copy(alpha = 0.4f) else theme.contentColor.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (video?.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = video.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (video != null) {
                                    Icon(Icons.Outlined.PlayArrow, null, tint = accent.copy(alpha = 0.6f))
                                } else {
                                    // Placeholder vide discret (v9.2.7)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(theme.contentColor.copy(alpha = 0.05f), CircleShape)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = video?.title ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = theme.contentColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.heightIn(min = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun PresentationVideoPlayerDialog(
    video: PresentationVideo,
    onDismiss: () -> Unit,
    theme: AppThemeState
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(video.title, color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, null, tint = Color.White)
                    }
                }
                
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    VideoPlayerBanner(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16/9f),
                        overrideVideoUrl = video.videoUrl, // v9.2.7 : Lecture de l'URL spécifique
                        onDismiss = {} // Non applicable ici
                    )
                }
            }
        }
    }
}
