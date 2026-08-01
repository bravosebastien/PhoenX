package com.example.phoenx.ui.screens.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LibraryCoverPickerScreen(
    compartmentId: String,
    compartmentName: String,
    navController: NavController,
    viewModel: LibraryCoverViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val covers by viewModel.covers.collectAsState()
    val existingCover = covers[compartmentId]

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedType by remember { mutableStateOf("none") }
    
    // Métadonnées de positionnement (v9.4.19)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Résolution de l'URL existante (v9.4.17)
    val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(context, com.example.phoenx.data.media.MediaManager.MediaManagerEntryPoint::class.java)
    val mediaManager = entryPoint.mediaManager()
    var resolvedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingCover) {
        if (existingCover != null) {
            selectedType = existingCover.mediaType
            scale = existingCover.scale
            offsetX = existingCover.offsetX
            offsetY = existingCover.offsetY
            resolvedUrl = mediaManager.getSafeUrl(existingCover.mediaUrl)
        }
    }

    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedType = "photo"
            // Reset position lors d'une nouvelle sélection
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            resolvedUrl = null
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedType = "video"
            resolvedUrl = null
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Personnaliser $compartmentName", style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Choisis une photo ou une courte vidéo qui représente cet espace pour toi.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // DEUX BOUTONS CÔTE À CÔTE
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("📷 Photo", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { videoLauncher.launch("video/*") },
                    modifier = Modifier.weight(1f).height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Text("🎬 Vidéo", color = accent, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // APERÇU
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(theme.contentColor.copy(alpha = 0.05f), MaterialTheme.shapes.large)
                    .border(1.dp, theme.contentColor.copy(alpha = 0.1f), MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
                    .pointerInput(selectedUri, resolvedUrl) {
                        if (selectedType == "photo") {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val displayModel = selectedUri ?: resolvedUrl
                
                if (displayModel != null) {
                    if (selectedType == "photo") {
                        AsyncImage(
                            model = displayModel,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Pour la vidéo, on affiche l'aperçu si on a une URI locale, sinon un placeholder
                        if (selectedUri != null) {
                            VideoPreview(uri = selectedUri!!)
                        } else {
                            Icon(Icons.Default.VideoLibrary, null, tint = accent.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        }
                    }
                } else {
                    Text("Aucun média sélectionné", color = theme.contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }

            if (selectedUri != null || resolvedUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val baseMsg = if (selectedType == "photo") "Cette image s'affichera sur la carte." else "Cette vidéo défilera en silence sur la carte."
                val instructions = if (selectedType == "photo") " Pince pour zoomer, glisse pour cadrer." else ""
                Text(
                    text = baseMsg + instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.5f),
                    fontStyle = FontStyle.Italic,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isUploading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = accent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Envoi en cours... ${(uploadProgress * 100).toInt()}%", color = theme.contentColor.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { 
                    if (selectedUri != null) {
                        viewModel.uploadCover(compartmentId, selectedUri!!, selectedType, scale, offsetX, offsetY)
                        Toast.makeText(context, "Couverture mise à jour.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else if (resolvedUrl != null) {
                        // Cas particulier : l'utilisateur a modifié le cadrage d'une image déjà uploadée (v9.4.19)
                        // On uploade uniquement les métadonnées
                        viewModel.updateCoverMetadata(compartmentId, scale, offsetX, offsetY)
                        Toast.makeText(context, "Cadrage mis à jour.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    },
                    enabled = selectedUri != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Enregistrer cette couverture", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { 
                    viewModel.deleteCover(compartmentId)
                    Toast.makeText(context, "Illustration par défaut restaurée.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }) {
                    Text("Supprimer la personnalisation", color = Error.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@UnstableApi
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
