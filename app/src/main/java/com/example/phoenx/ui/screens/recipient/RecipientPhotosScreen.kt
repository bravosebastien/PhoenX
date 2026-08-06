package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.layout.ContentScale
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.*
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientPhotosScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel(),
    standaloneViewModel: com.example.phoenx.ui.screens.library.LiteraryLibraryViewModel = hiltViewModel(),
    themeViewModel: com.example.phoenx.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val entries by viewModel.archiveEntries.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = androidx.compose.ui.platform.LocalContext.current
    val heirKey by viewModel.heirKey.collectAsState()
    
    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    val isCreatorMode = creatorId == null || creatorId == viewModel.currentUid
    var showAddDialog by remember { mutableStateOf(false) }
    var showInfoPopup by remember { mutableStateOf(false) }
    var editingMedia by remember { mutableStateOf<PhoenXEntry?>(null) } // v9.4.27
    var mediaToDelete by remember { mutableStateOf<PhoenXEntry?>(null) }
    val recipients by standaloneViewModel.recipients.collectAsState()

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    if (isCreatorMode) {
        com.example.phoenx.ui.components.OnboardingPopup(
            pageKey = "phototheque",
            title = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getTitle("PHOTO"),
            contentPoints = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getContent("PHOTO"),
            preferenceManager = themeViewModel.preferenceManager
        )
    }

    if (showInfoPopup) {
        AlertDialog(
            onDismissRequest = { showInfoPopup = false },
            containerColor = theme.backgroundColor,
            title = { Text(com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getTitle("PHOTO"), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getContent("PHOTO").forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = accent, modifier = Modifier.padding(end = 8.dp))
                            Text(point, style = MaterialTheme.typography.bodyMedium, color = theme.contentColor.copy(alpha = 0.8f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showInfoPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Fermer", color = theme.backgroundColor)
                }
            }
        )
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Grande Photothèque", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        if (isCreatorMode) {
                            IconButton(onClick = { showInfoPopup = true }) {
                                Icon(Icons.Default.Info, null, tint = accent)
                            }
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, null, tint = accent)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                
                // BANDEAU D'AIDE (v9.3.2)
                if (isCreatorMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        color = accent.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Note : Ce média sera déposé seul. Pour l'associer à un souvenir précis, utilisez l'écran de Capture.",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("L'album est encore vide.", color = theme.contentColor.copy(alpha = 0.4f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        PhotoItem(
                            entry = entry, 
                            theme = theme,
                            isCreatorMode = isCreatorMode,
                            heirKey = heirKey,
                            mediaManager = mediaManager,
                            onDelete = { mediaToDelete = entry },
                            onEdit = { if (isCreatorMode) editingMedia = entry } // v9.4.27
                        ) { 
                            android.util.Log.d("MediaSupportDiag", "Clic Photo - ID: ${entry.id}, Title: ${entry.aiSummary}")
                            onNavigateToDetail(entry.id) 
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        com.example.phoenx.ui.components.DirectPhotoDialog(
            recipients = recipients,
            onDismiss = { showAddDialog = false },
            onSave = { title, userComment, uri, ids ->
                // Conversion Uri -> File temporaire pour upload
                val tempFile = java.io.File(context.cacheDir, "standalone_photo_${java.util.UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.uploadAndAddStandalonePhoto(title, userComment, tempFile, ids)
                showAddDialog = false
            }
        )
    }

    if (mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text("Supprimer cette photo ?", color = theme.contentColor) },
            text = { Text("Cette action est irréversible.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMediaEntry(mediaToDelete!!)
                        mediaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.phoenx.ui.theme.Error)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mediaToDelete = null }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }

    if (editingMedia != null) {
        val isComplement = editingMedia!!.parentEntryId != null
        
        com.example.phoenx.ui.components.DirectMediaDialog(
            type = "PHOTO",
            recipients = recipients,
            onDismiss = { editingMedia = null },
            onSave = { title, comment, url, ids, visibility ->
                viewModel.updateMediaEntry(editingMedia!!.id, title, comment, url, ids, visibility, isComplement)
                editingMedia = null
            },
            initialTitle = editingMedia!!.aiSummary,
            initialUserComment = editingMedia!!.userComment,
            initialUrl = editingMedia!!.mediaUrl ?: "",
            initialRecipientIds = editingMedia!!.recipientIds,
            initialVisibility = editingMedia!!.visibility
        )
    }
}

@Composable
fun PhotoItem(
    entry: PhoenXEntry, 
    theme: AppThemeState, 
    isCreatorMode: Boolean,
    heirKey: ByteArray?,
    mediaManager: MediaManager,
    onDelete: () -> Unit,
    onEdit: () -> Unit, // v9.4.27
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(theme.contentColor.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .phoenXMatiere(),
        contentAlignment = Alignment.Center
    ) {
        // AFFICHAGE SÉCURISÉ DE LA MINIATURE (v9.4.27)
        SecureAsyncImage(
            mediaUrl = entry.mediaUrl,
            localPath = entry.localMediaPath,
            explicitKey = heirKey,
            mediaManager = mediaManager,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay pour le titre en bas
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .padding(4.dp)
        ) {
            Text(
                text = entry.aiSummary.ifEmpty { "Photo" },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        // Boutons Actions (v9.4.27)
        if (isCreatorMode) {
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
