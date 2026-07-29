package com.example.phoenx.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.phoenx.ui.theme.LocalAppTheme
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

@UnstableApi
@Composable
fun VideoPlayerBanner(
    modifier: Modifier = Modifier,
    overrideVideoUrl: String? = null, // v9.2.7 : Support URL externe
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var videoUrl by remember { mutableStateOf(overrideVideoUrl ?: "") }
    var isMuted by remember { mutableStateOf(overrideVideoUrl == null) } // Muet par défaut sur l'accueil uniquement
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(false) }

    // Remote Config fetching (Uniquement si pas d'override)
    LaunchedEffect(overrideVideoUrl) {
        if (overrideVideoUrl == null) {
            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val url = remoteConfig.getString("home_video_url").trim()
                    if (url.isNotEmpty()) videoUrl = url
                }
            }
        } else {
            videoUrl = overrideVideoUrl
        }
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
    }

    // Mise à jour de la vidéo quand l'URL change
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl.toUri()))
            exoPlayer.prepare()
        }
    }

    // Gestion du son
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Gestion du Play/Pause
    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showControls = !showControls }
        ) {
            if (videoUrl.isEmpty()) {
                // ... (placeholder stays as is, or maybe showControls should be true by default here?)
                // Actually, if it's empty, we might want to keep the close button visible to allow dismissal.
                // But let's follow the requirement: controls appear on tap.
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.contentColor.copy(alpha = 0.05f)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune vidéo configurée",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )
                }
            } else {
                // Vidéo Player
                AndroidView(
                    factory = { ctx ->
                        @androidx.media3.common.util.UnstableApi
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- CONTRÔLES (v9.2.7 : Auto-hide avec fondu) ---
            
            Column { // Fournit un ColumnScope pour AnimatedVisibility (v9.2.7)
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Voile léger pour améliorer la visibilité des contrôles
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

                        // Bouton Fermer (Haut Droite)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onDismiss() },
                            color = theme.backgroundColor.copy(alpha = 0.7f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = theme.contentColor,
                                modifier = Modifier.padding(8.dp).size(18.dp)
                            )
                        }

                        if (videoUrl.isNotEmpty()) {
                            // Bouton Son (Bas Gauche)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { isMuted = !isMuted },
                                color = theme.backgroundColor.copy(alpha = 0.7f)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Son",
                                    tint = theme.contentColor,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }

                            // Bouton Play/Pause (Bas Centre)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { isPlaying = !isPlaying },
                                color = theme.backgroundColor.copy(alpha = 0.7f)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = theme.contentColor,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
