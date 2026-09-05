package com.example.phoenx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*

/**
 * Composant Cameo (v8.9.9) : Affiche le portrait d'un proche dans un ovale "médaillon"
 * avec un filtre artistique "Portrait au Fusain" (Grayscale + High Contrast).
 * Supporte désormais la résolution de chemin Storage (v9.4.17).
 */
@Composable
fun CameoPortrait(
    imagePath: String?,
    firstName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    resolvedUrl: String? = null, // v9.4.27 : URL déjà résolue (Source unique)
    creatorId: String? = null, // v9.6.6
    docType: String? = null,
    docId: String? = null,
    field: String? = null,
    explicitKey: ByteArray? = null,
    isEncrypted: Boolean = false,
    hideIfEmpty: Boolean = false // v9.6.7
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = androidx.compose.ui.platform.LocalContext.current

    // v9.4.27 : État de résolution interne uniquement si resolvedUrl est absent
    var internalUrl by remember(imagePath) { mutableStateOf<String?>(null) }
    
    // SOURCE DE VÉRITÉ : resolvedUrl prioritaire > résolution interne > path brut (v9.4.28: Fix file://)
    val fallbackUrl = remember(imagePath) {
        if (!imagePath.isNullOrBlank() && !imagePath.startsWith("http") && !imagePath.startsWith("file://") && java.io.File(imagePath).exists()) {
            "file://$imagePath"
        } else imagePath
    }
    val displayUrl = resolvedUrl ?: internalUrl ?: fallbackUrl

    val mediaManager = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    LaunchedEffect(imagePath, resolvedUrl, creatorId) {
        // SÉCURITÉ : Si une URL résolue est fournie (ViewModel) OU si nous sommes en mode Destinataire (creatorId présent),
        // on ne déclenche AUCUNE résolution locale. On laisse SecureAsyncImage gérer la résolution sécurisée.
        if (resolvedUrl == null && creatorId == null && !imagePath.isNullOrBlank()) {
            android.util.Log.d("PHOENX_TREE_TRACE", "Résolution interne Créateur pour $firstName ($imagePath)")

            if (java.io.File(imagePath).exists()) {
                internalUrl = if (imagePath.startsWith("file://")) imagePath else "file://$imagePath"
            } else {
                try {
                    internalUrl = mediaManager.getSafeUrl(imagePath)
                } catch (e: Exception) {
                    android.util.Log.w("PHOENX_TREE_TRACE", "Échec getSafeUrl pour $firstName")
                }
            }
        } else if (resolvedUrl != null) {
            android.util.Log.d("PHOENX_TREE_TRACE", "Usage de resolvedUrl pour $firstName: $resolvedUrl")
        }
    }

    // Forme Ovale Cameo (plus haut que large, ratio ~1.2)
    val cameoShape = GenericShape { size, _ ->
        addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
    }

    Box(
        modifier = if (hideIfEmpty && displayUrl.isNullOrBlank()) Modifier.size(0.dp) else modifier
            .size(width = size, height = size * 1.25f)
            .clip(cameoShape)
            .background(theme.contentColor.copy(alpha = 0.05f))
            .border(1.dp, accent.copy(alpha = 0.3f), cameoShape),
        contentAlignment = Alignment.Center
    ) {
        if (!displayUrl.isNullOrBlank()) {
            val isPathEncrypted = displayUrl?.endsWith(".enc") == true
            val isLocal = displayUrl?.let { it.startsWith("/") || it.startsWith("file://") } == true
            val cleanLocalPath = if (displayUrl?.startsWith("file://") == true) displayUrl.substring(7) else if (displayUrl?.startsWith("/") == true) displayUrl else null

            SecureAsyncImage(
                mediaUrl = if (isLocal && !isPathEncrypted) null else displayUrl,
                localPath = cleanLocalPath,
                mediaManager = mediaManager,
                explicitKey = explicitKey,
                creatorId = creatorId,
                docType = docType,
                docId = docId,
                field = field,
                isEncrypted = isEncrypted || isPathEncrypted,
            )
        } else if (!hideIfEmpty) {
            // Placeholder Initiale (Couleur accent)
            Text(
                text = firstName.take(1).uppercase(),
                color = accent, 
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4).sp,
                    fontFamily = theme.fontFamily
                )
            )
        }
    }
}
