package com.example.phoenx.ui.components

import android.util.Log
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
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null, // v9.4.29
    creatorId: String? = null, // v9.4.27
    docType: String? = null,   // v9.4.27
    docId: String? = null,     // v9.4.27
    field: String? = null,     // v9.4.27
    personId: String? = null,  // v9.6.6 : Pour résolution personMedia
    isEncrypted: Boolean = true // v9.4.29 : Support pour médias non-chiffrés (Cameos)
) {
    var imageBytes by remember(mediaUrl, localPath) { mutableStateOf<ByteArray?>(null) }
    var resolvedSimpleUrl by remember(mediaUrl) { mutableStateOf<String?>(null) } // v9.4.29
    var isLoading by remember(mediaUrl, localPath) { mutableStateOf(false) }

    LaunchedEffect(mediaUrl, localPath, explicitKey, creatorId, docType, docId, field, personId) {
        if (imageBytes != null || resolvedSimpleUrl != null) return@LaunchedEffect 

        if (localPath != null && java.io.File(localPath).exists()) {
            Log.d("PHOENX_SECURE_IMG", "Usage fichier local: docId=$docId, path=$localPath")
            return@LaunchedEffect
        }
        
        if (mediaUrl != null) {
            Log.d("PHOENX_SECURE_IMG", "Début résolution: docId=$docId, url=$mediaUrl, encrypted=$isEncrypted")
            isLoading = true
            try {
                if (isEncrypted) {
                    val bytes = withContext(Dispatchers.IO) {
                        mediaManager.downloadAndDecrypt(
                            mediaUrl, 
                            explicitKey,
                            creatorId,
                            docType,
                            docId,
                            field,
                            personId
                        )
                    }
                    imageBytes = bytes
                    Log.d("PHOENX_SECURE_IMG", "Résolution chiffrée réussie: docId=$docId, size=${bytes.size} bytes")
                } else {
                    // v9.4.29 : Pour les cameos, on résout simplement le chemin Storage en URL signée
                    // On passe les paramètres de sécurité pour supporter les Destinataires
                    val safeUrl = mediaManager.getSafeUrl(
                        pathOrUrl = mediaUrl,
                        explicitKey = if (creatorId != null) byteArrayOf(0) else null, // Flag pour mode Recipient si creatorId présent
                        creatorId = creatorId,
                        docType = docType,
                        docId = docId,
                        field = field,
                        personId = personId
                    )
                    resolvedSimpleUrl = safeUrl
                    Log.d("PHOENX_SECURE_IMG", "Résolution simple réussie: docId=$docId, url=$safeUrl")
                }
            } catch (e: Exception) {
                Log.e("PHOENX_SECURE_IMG", "Erreur résolution image: docId=$docId, msg=${e.message}")
            } finally {
                isLoading = false
            }
        } else {
            Log.w("PHOENX_SECURE_IMG", "Aucun chemin ni URL pour docId=$docId")
        }
    }

    Box(modifier = modifier) {
        val model: Any? = when {
            localPath != null && java.io.File(localPath).exists() -> localPath
            imageBytes != null -> imageBytes
            resolvedSimpleUrl != null -> resolvedSimpleUrl
            else -> null
        }

        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                colorFilter = colorFilter
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
