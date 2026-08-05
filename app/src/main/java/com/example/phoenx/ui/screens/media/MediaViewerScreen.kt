package com.example.phoenx.ui.screens.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.core.net.toUri
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.BackgroundPrimary

@UnstableApi
@Composable
fun MediaViewerScreen(
    entryId: String,
    creatorId: String?,
    onExit: () -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val heirKey by viewModel.heirKey.collectAsState()

    LaunchedEffect(entryId, creatorId) {
        viewModel.loadMedia(entryId, creatorId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        android.util.Log.d("MediaSupportDiag", "État Entry: ${if (entry == null) "NULL" else "Présent (ID: ${entry!!.id}, Type: ${entry!!.type})"}")
        
        if (entry != null) {
            android.util.Log.d("MediaSupportDiag", "Détails Entry - LocalPath: ${entry!!.localMediaPath}, MediaUrl: ${entry!!.mediaUrl}")

            when (entry!!.type) {
                com.example.phoenx.domain.model.EntryType.PHOTO -> {
                    android.util.Log.d("MediaSupportDiag", "Branche PHOTO choisie")
                    ZoomableImage(
                        mediaUrl = entry!!.mediaUrl,
                        localPath = entry!!.localMediaPath,
                        explicitKey = heirKey,
                        mediaManager = viewModel.mediaManager
                    )
                }
                com.example.phoenx.domain.model.EntryType.VIDEO -> {
                    android.util.Log.d("MediaSupportDiag", "Branche VIDEO choisie")
                    VideoPlayer(
                        mediaUrl = entry!!.mediaUrl,
                        localPath = entry!!.localMediaPath,
                        explicitKey = heirKey,
                        mediaManager = viewModel.mediaManager
                    )
                }
                com.example.phoenx.domain.model.EntryType.AUDIO,
                com.example.phoenx.domain.model.EntryType.EMOTION -> {
                    android.util.Log.d("MediaSupportDiag", "Branche AUDIO/EMOTION choisie")
                    AudioPlayer(
                        mediaUrl = entry!!.mediaUrl,
                        localPath = entry!!.localMediaPath,
                        explicitKey = heirKey,
                        mediaManager = viewModel.mediaManager,
                        title = entry!!.aiSummary
                    )
                }
                else -> {
                    android.util.Log.e("MediaSupportDiag", "Branche ELSE atteinte. Type inconnu: ${entry!!.type}")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Média non supporté (${entry!!.type})", color = Color.White)
                    }
                }
            }
        } else {
            android.util.Log.d("MediaSupportDiag", "Entry est NULL, affichage loader")
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentPrimary)
        }

        // BOUTON FERMER
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}

@Composable
fun ZoomableImage(
    mediaUrl: String?,
    localPath: String?,
    explicitKey: ByteArray?,
    mediaManager: com.example.phoenx.data.media.MediaManager
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = state)
            .graphicsLayer(
                scaleX = scale.coerceIn(1f, 5f),
                scaleY = scale.coerceIn(1f, 5f),
                translationX = offset.x,
                translationY = offset.y
            ),
        contentAlignment = Alignment.Center
    ) {
        SecureAsyncImage(
            mediaUrl = mediaUrl,
            localPath = localPath,
            explicitKey = explicitKey,
            mediaManager = mediaManager,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@UnstableApi
@Composable
fun VideoPlayer(
    mediaUrl: String?,
    localPath: String?,
    explicitKey: ByteArray?,
    mediaManager: com.example.phoenx.data.media.MediaManager
) {
    val context = LocalContext.current
    var resolvedUrl by remember(mediaUrl, localPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaUrl, localPath) {
        resolvedUrl = localPath ?: mediaManager.getSafeUrl(mediaUrl)
    }

    if (resolvedUrl == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentPrimary)
        }
        return
    }

    val exoPlayer = remember(resolvedUrl) {
        ExoPlayer.Builder(context).build().apply {
            val isLocal = localPath != null
            android.util.Log.d("MediaViewerDiag", "Configuration Player - Local: $isLocal, Path: $localPath, Url: $mediaUrl")
            
            val factory = if (isLocal) {
                android.util.Log.d("MediaViewerDiag", "Utilisation DefaultDataSource (Fichier local)")
                androidx.media3.datasource.DefaultDataSource.Factory(context)
            } else {
                android.util.Log.d("MediaViewerDiag", "Utilisation EncryptedMediaDataSource (Distant/Chiffré)")
                mediaManager.getEncryptedDataSourceFactory(explicitKey)
            }
            
            val uri = resolvedUrl!!.toUri()
            val mediaSource = ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(uri))
            
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("MediaViewerDiag", "ERREUR ExoPlayer: ${error.message}", error)
                    android.util.Log.e("MediaViewerDiag", "Cause: ${error.cause?.message}")
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateStr = when(playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN"
                    }
                    android.util.Log.d("MediaViewerDiag", "État Player: $stateStr")
                }
            })

            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@UnstableApi
@Composable
fun AudioPlayer(
    mediaUrl: String?,
    localPath: String?,
    explicitKey: ByteArray?,
    mediaManager: com.example.phoenx.data.media.MediaManager,
    title: String
) {
    val context = LocalContext.current
    var resolvedUrl by remember(mediaUrl, localPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaUrl, localPath) {
        android.util.Log.d("MediaViewerDiag", "AudioPlayer - Début résolution URL pour MediaUrl: $mediaUrl, LocalPath: $localPath")
        resolvedUrl = localPath ?: mediaManager.getSafeUrl(mediaUrl)
        android.util.Log.d("MediaViewerDiag", "AudioPlayer - URL résolue: $resolvedUrl")
    }

    if (resolvedUrl == null) {
        android.util.Log.d("MediaViewerDiag", "AudioPlayer - Affichage indicateur (resolvedUrl est NULL)")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentPrimary)
        }
        return
    }

    val exoPlayer = remember(resolvedUrl) {
        ExoPlayer.Builder(context).build().apply {
            val isLocal = localPath != null
            android.util.Log.d("MediaViewerDiag", "Configuration AudioPlayer - Local: $isLocal, Path: $localPath, Url: $mediaUrl")

            val factory = if (isLocal) {
                android.util.Log.d("MediaViewerDiag", "Utilisation DefaultDataSource (Fichier local)")
                androidx.media3.datasource.DefaultDataSource.Factory(context)
            } else {
                android.util.Log.d("MediaViewerDiag", "Utilisation EncryptedMediaDataSource (Distant/Chiffré)")
                mediaManager.getEncryptedDataSourceFactory(explicitKey)
            }

            val uri = resolvedUrl!!.toUri()
            val mediaSource = ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(uri))

            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("MediaViewerDiag", "ERREUR Audio ExoPlayer: ${error.message}", error)
                    android.util.Log.e("MediaViewerDiag", "Cause: ${error.cause?.message}")
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateStr = when(playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN"
                    }
                    android.util.Log.d("MediaViewerDiag", "État Audio Player: $stateStr")
                }
            })

            setMediaSource(mediaSource)
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = AccentPrimary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, null, tint = AccentPrimary, modifier = Modifier.size(64.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        IconButton(
            onClick = { 
                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                isPlaying = !isPlaying
            },
            modifier = Modifier.size(80.dp).background(AccentPrimary, CircleShape)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                null, 
                tint = BackgroundPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
