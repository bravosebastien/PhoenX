package com.example.phoenx.ui.screens.settings

import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.components.VideoPlayerBanner
import com.example.phoenx.ui.screens.recipient.RecipientMediaViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_LEGACY_VIDEO_DURATION_MS = 5 * 60 * 1000L // 5 minutes

/**
 * v12.6 : Écran de dépôt de la vidéo de présentation de l'héritage.
 * Une seule vidéo par Créateur, visible par tous ses Destinataires une fois l'héritage
 * activé (voir RecipientMediaViewModel.saveLegacyVideo / legacyVideoUrl).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyVideoScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val legacyVideoUrl by viewModel.legacyVideoUrl.collectAsState()

    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.setTargetCreator(null)
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        errorMessage = null
        isUploading = true
        scope.launch {
            try {
                val tempFile = File(context.cacheDir, "legacy_video_${System.currentTimeMillis()}.mp4")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                val retriever = MediaMetadataRetriever()
                val durationMs = try {
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } finally {
                    retriever.release()
                }

                if (durationMs > MAX_LEGACY_VIDEO_DURATION_MS) {
                    errorMessage = "Cette vidéo dure plus de 5 minutes. Choisissez une vidéo plus courte."
                    tempFile.delete()
                } else {
                    viewModel.saveLegacyVideo(tempFile)
                }
            } catch (e: Exception) {
                errorMessage = "Impossible de préparer cette vidéo. Réessayez avec un autre fichier."
            } finally {
                isUploading = false
            }
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Vidéo de mon héritage", color = theme.contentColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Une seule vidéo, pour raconter le pourquoi de votre héritage. Elle sera visible par toutes les personnes qui recevront ce que vous avez construit ici — sans distinction entre elles. Maximum 5 minutes.",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = theme.contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (legacyVideoUrl != null) {
                VideoPlayerBanner(
                    modifier = Modifier.fillMaxWidth(),
                    overrideVideoUrl = legacyVideoUrl,
                    onDismiss = {}
                )

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.removeLegacyVideo() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer cette vidéo")
                }

                Spacer(Modifier.height(12.dp))

                TextButton(onClick = { videoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }) {
                    Text("Remplacer par une autre vidéo", color = accent)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = theme.contentColor.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (isUploading) {
                                CircularProgressIndicator(color = accent)
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.VideoCall, contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Aucune vidéo pour l'instant", color = theme.contentColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { videoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(if (isUploading) "Envoi en cours..." else "Choisir une vidéo")
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
