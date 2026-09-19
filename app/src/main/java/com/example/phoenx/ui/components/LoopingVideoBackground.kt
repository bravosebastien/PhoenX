package com.example.phoenx.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.phoenx.R

/**
 * LoopingVideoBackground (v12.7.2)
 * Affiche une vidéo en boucle infinie, muette et sans contrôles.
 * Utilisé comme fond animé pour les cartes d'accueil.
 * v12.7.3 : Passage en TextureView via XML pour supporter les transformations Compose (graphicsLayer).
 */
@UnstableApi
@Composable
fun LoopingVideoBackground(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("PHOENX_VIDEO_DEBUG", "Erreur lecture vidéo: ${error.message}", error)
                }
            })
        }
    }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            android.util.Log.d("PHOENX_VIDEO_DEBUG", "LoopingVideoBackground: Loading URL = $videoUrl")
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl.toUri()))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            val playerView = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.view_looping_video_player, null) as PlayerView
            playerView.player = exoPlayer
            playerView
        },
        modifier = modifier
    )
}

/**
 * Utilitaire pour détecter si une URL pointe vers une vidéo.
 */
fun isVideoUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val cleanUrl = url.substringBefore("?").lowercase()
    return cleanUrl.contains(".mp4") || cleanUrl.contains(".mov") || cleanUrl.contains(".webm")
}
