package com.example.phoenx.ui.screens.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun LibraryCoverPickerScreen(
    compartmentId: String,
    compartmentName: String,
    navController: NavController,
    viewModel: LibraryCoverViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedType by remember { mutableStateOf("none") }
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedType = "photo"
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedType = "video"
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Personnaliser $compartmentName", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Choisis une photo ou une courte vidéo qui représente cet espace pour toi.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // DEUX BOUTONS CÔTE À CÔTE
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("📷 Photo", color = BackgroundPrimary)
                }
                OutlinedButton(
                    onClick = { videoLauncher.launch("video/*") },
                    modifier = Modifier.weight(1f).height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary)
                ) {
                    Text("🎬 Vidéo", color = AccentPrimary)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // APERÇU
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(SurfaceCard, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    if (selectedType == "photo") {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        VideoPreview(uri = selectedUri!!)
                    }
                } else {
                    Text("Aucun média sélectionné", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (selectedUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (selectedType == "photo") "Cette image s'affichera sur la carte." else "Cette vidéo défilera en silence sur la carte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isUploading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Envoi en cours... ${(uploadProgress * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = { 
                        if (selectedUri != null) {
                            viewModel.uploadCover(compartmentId, selectedUri!!, selectedType)
                            Toast.makeText(context, "Couverture mise à jour.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    enabled = selectedUri != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Enregistrer cette couverture", color = BackgroundPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { 
                    viewModel.deleteCover(compartmentId)
                    Toast.makeText(context, "Illustration par défaut restaurée.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }) {
                    Text("Supprimer la personnalisation", color = Color(0xFFE57373))
                }
            }
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoPreview(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // Règle d'or : MUET
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

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
