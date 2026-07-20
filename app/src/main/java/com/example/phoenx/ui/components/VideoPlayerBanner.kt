package com.example.phoenx.ui.components

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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.BackgroundPrimary
import com.example.phoenx.ui.theme.TextPrimary
import com.example.phoenx.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerBanner(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var videoUrl by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }

    // Remote Config fetching
    LaunchedEffect(Unit) {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0 // Force le rafraîchissement immédiat pour les tests
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        android.util.Log.d("PHOENX_VIDEO", "Tentative de récupération Remote Config...")
        
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val keys = remoteConfig.all.keys
                android.util.Log.d("PHOENX_VIDEO", "Fetch réussi ! Clés disponibles : ${keys.joinToString(", ")}")
                
                val url = remoteConfig.getString("home_video_url").trim()
                if (url.isNotEmpty()) {
                    android.util.Log.d("PHOENX_VIDEO", "URL trouvée : $url")
                    videoUrl = url
                } else {
                    android.util.Log.w("PHOENX_VIDEO", "Attention : La clé 'home_video_url' existe mais la valeur est VIDE.")
                }
            } else {
                val error = task.exception?.message ?: "Erreur inconnue"
                android.util.Log.e("PHOENX_VIDEO", "Échec du Fetch : $error")
            }
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
        Box(modifier = Modifier.fillMaxSize()) {
            if (videoUrl.isEmpty()) {
                // Placeholder
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2E2E35)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune vidéo configurée",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                // Vidéo Player
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- CONTRÔLES ---
            
            // Bouton Fermer (Haut Droite)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() },
                color = BackgroundPrimary.copy(alpha = 0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = TextPrimary,
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
                    color = BackgroundPrimary.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Son",
                        tint = TextPrimary,
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
                    color = BackgroundPrimary.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = TextPrimary,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
        }
    }
}
