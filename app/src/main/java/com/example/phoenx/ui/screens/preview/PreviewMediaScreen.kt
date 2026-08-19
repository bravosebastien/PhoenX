package com.example.phoenx.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import com.example.phoenx.ui.theme.phoenXMatiere
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewMediaScreen(
    type: String,
    recipientUid: String,
    onNavigateBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    LaunchedEffect(recipientUid) {
        viewModel.loadPreview(recipientUid)
    }

    val filteredList = remember(state.filteredMedia, type) {
        val entryType = when(type) {
            "PHOTO" -> EntryType.PHOTO
            "VIDEO" -> EntryType.VIDEO
            "AUDIO" -> EntryType.AUDIO
            else -> EntryType.PHOTO
        }
        state.filteredMedia.filter { it.type == entryType }
    }

    // GESTION PANNEAU MÉDIA (v9.4.27)
    var selectedMediaId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    
    val selectedEntry = remember(selectedMediaId, state.allFilteredEntries) {
        state.allFilteredEntries.find { it.id == selectedMediaId }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val titleLabel = when(type) {
                            "PHOTO" -> "Photothèque"
                            "VIDEO" -> "Vidéothèque"
                            "AUDIO" -> "Discothèque"
                            else -> "Médiathèque"
                        }
                        Text("$titleLabel (Aperçu)", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(state.recipientName, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun média partagé avec ${state.recipientName}", color = theme.contentColor.copy(alpha = 0.4f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredList) { entry ->
                    PreviewMediaCard(
                        entry = entry, 
                        theme = theme, 
                        mediaManager = mediaManager,
                        onClick = { 
                            if (entry.mediaUrl?.startsWith("http") == true) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(entry.mediaUrl!!))
                                    context.startActivity(intent)
                                } catch(_: Exception) { /* ... */ }
                            } else {
                                selectedMediaId = entry.id
                                showSheet = true
                            }
                        }
                    )
                }
            }
        }

        if (showSheet && selectedEntry != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = theme.backgroundColor,
                scrimColor = Color.Black.copy(alpha = 0.5f)
            ) {
                @androidx.media3.common.util.UnstableApi
                PreviewMediaPanel(
                    entry = selectedEntry,
                    mediaManager = mediaManager,
                    onDismiss = { showSheet = false }
                )
            }
        }
    }
}

@Composable
fun PreviewMediaCard(
    entry: PhoenXEntry, 
    theme: com.example.phoenx.ui.theme.AppThemeState,
    mediaManager: MediaManager,
    onClick: () -> Unit // v9.4.27
) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onClick() }.phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            val displayUrl = entry.coverUrl ?: entry.mediaUrl
            val displayPath = entry.localCoverPath ?: entry.localMediaPath
            
            if (displayUrl != null || displayPath != null) {
                SecureAsyncImage(
                    mediaUrl = displayUrl,
                    localPath = displayPath,
                    mediaManager = mediaManager,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    creatorId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
                    docType = entry.sourceDocType,
                    docId = entry.id,
                    field = if (entry.coverUrl != null) "coverUrl" else null
                )
            } else {
                val icon = when(entry.type) {
                    EntryType.PHOTO -> Icons.Default.PhotoLibrary
                    EntryType.VIDEO -> Icons.Default.Movie
                    EntryType.AUDIO -> Icons.Default.Album
                    else -> Icons.Default.PhotoLibrary
                }
                Icon(icon, null, tint = theme.accentColor.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
            }
            
            // Titre en overlay léger
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = entry.aiSummary.ifBlank { "Média" },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
