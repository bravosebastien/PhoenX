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
    isEncrypted: Boolean = true, // v9.4.29 : Support pour médias non-chiffrés (Cameos)
    hideIfEmpty: Boolean = false // v9.6.7 : Permet de masquer totalement le bloc si vide (Livre)
) {
    var imageBytes by remember(docId) { mutableStateOf<ByteArray?>(null) }
    var resolvedSimpleUrl by remember(docId) { mutableStateOf<String?>(null) }
    var isLoading by remember(docId, mediaUrl, localPath) { mutableStateOf(false) }

    // v9.6.7 : Détection automatique des chemins locaux injectés dans mediaUrl
    val effectiveLocalPath = remember(localPath, mediaUrl) {
        localPath ?: if (mediaUrl?.let { it.startsWith("/") || it.startsWith("file://") } == true) {
            if (mediaUrl.startsWith("file://")) mediaUrl.substring(7) else mediaUrl
        } else null
    }

    LaunchedEffect(mediaUrl, localPath, explicitKey, creatorId, docType, docId, field, personId) {
        // On ne reset PAS imageBytes/resolvedSimpleUrl si le docId est le même, 
        // pour éviter le trou visuel pendant la nouvelle résolution.
        
        if (effectiveLocalPath != null && java.io.File(effectiveLocalPath).exists()) {
            Log.d("PHOENX_SECURE_IMG", "Usage fichier local: docId=$docId, path=$effectiveLocalPath")
            return@LaunchedEffect
        }
        
        if (mediaUrl != null && !mediaUrl.startsWith("/") && !mediaUrl.startsWith("file://")) {
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
                    // v9.4.29 : Pour les cameos, on résout le chemin Storage en URL signée (sauf si déjà URL http)
                    if (mediaUrl.startsWith("http")) {
                        resolvedSimpleUrl = mediaUrl
                    } else {
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
                    }
                    Log.d("PHOENX_SECURE_IMG", "Résolution simple réussie: docId=$docId, url=$resolvedSimpleUrl")
                }
            } catch (e: Exception) {
                Log.e("PHOENX_SECURE_IMG", "Erreur résolution image: docId=$docId, msg=${e.message}")
            } finally {
                isLoading = false
            }
        } else if (mediaUrl == null && effectiveLocalPath == null) {
            Log.w("PHOENX_SECURE_IMG", "Aucun chemin ni URL pour docId=$docId")
        }
    }

    // v9.6.7 : Calcul du modèle et persistance pour éviter le trou visuel (gap) lors d'un changement de chemin
    var lastSuccessfulModel by remember(docId) { mutableStateOf<Any?>(null) }
    
    val currentModel: Any? = remember(effectiveLocalPath, imageBytes, resolvedSimpleUrl, mediaUrl) {
        val model = when {
            effectiveLocalPath != null && java.io.File(effectiveLocalPath).exists() -> effectiveLocalPath
            imageBytes != null -> imageBytes
            resolvedSimpleUrl != null -> resolvedSimpleUrl
            mediaUrl?.startsWith("http") == true -> mediaUrl
            else -> null
        }
        if (model != null) lastSuccessfulModel = model
        model ?: lastSuccessfulModel
    }

    // On n'affiche le chargement que si on n'a vraiment rien à montrer et que c'est en cours
    val showLoading = isLoading && currentModel == null

    Box(modifier = if (hideIfEmpty && currentModel == null && !showLoading) Modifier.size(0.dp) else modifier) {
        if (currentModel != null) {
            AsyncImage(
                model = currentModel,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                colorFilter = colorFilter
            )
        } else if (showLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else if (!hideIfEmpty) {
            // Placeholder/Error state : visible uniquement si hideIfEmpty est false
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
        }
    }
}
