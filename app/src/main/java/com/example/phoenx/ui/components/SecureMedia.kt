package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.phoenx.data.media.MediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SecureAsyncImage (v8.4.5)
 * Gère le déchiffrement transparent des médias pour Coil.
 */
@Composable
fun SecureAsyncImage(
    mediaUrl: String?,
    localPath: String? = null,
    explicitKey: ByteArray? = null,
    mediaManager: MediaManager,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageBytes by remember(mediaUrl, localPath) { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember(mediaUrl, localPath) { mutableStateOf(false) }

    LaunchedEffect(mediaUrl, localPath) {
        if (localPath != null && java.io.File(localPath).exists()) {
            // Pas besoin de bytes si on a le chemin local
            return@LaunchedEffect
        }
        
        if (mediaUrl != null) {
            isLoading = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    mediaManager.downloadAndDecrypt(mediaUrl, explicitKey)
                }
                imageBytes = bytes
            } catch (e: Exception) {
                android.util.Log.e("SecureMedia", "Erreur déchiffrement image: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = modifier) {
        if (localPath != null && java.io.File(localPath).exists()) {
            AsyncImage(
                model = localPath,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (imageBytes != null) {
            AsyncImage(
                model = imageBytes,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            // Placeholder/Error state
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
        }
    }
}
