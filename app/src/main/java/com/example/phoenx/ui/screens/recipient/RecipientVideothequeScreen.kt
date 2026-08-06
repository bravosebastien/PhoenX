package com.example.phoenx.ui.screens.recipient

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.*
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientVideothequeScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel(),
    standaloneViewModel: com.example.phoenx.ui.screens.library.LiteraryLibraryViewModel = hiltViewModel(),
    themeViewModel: com.example.phoenx.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val entries by viewModel.videoEntries.collectAsState()
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
    var editingMedia by remember { mutableStateOf<PhoenXEntry?>(null) }
    var mediaToDelete by remember { mutableStateOf<PhoenXEntry?>(null) }
    var expandedMediaId by remember { mutableStateOf<String?>(null) } 
    
    val recipients by standaloneViewModel.recipients.collectAsState()

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Grande Vidéothèque", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (isCreatorMode) {
                        IconButton(onClick = { showInfoPopup = true }) { Icon(Icons.Default.Info, null, tint = accent) }
                        IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null, tint = accent) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Le projecteur est éteint...", color = theme.contentColor.copy(alpha = 0.4f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(entries) { entry ->
                        val isExpanded = expandedMediaId == entry.id
                        VHSCard(
                            entry = entry, 
                            theme = theme,
                            isCreatorMode = isCreatorMode,
                            isExpanded = isExpanded,
                            heirKey = heirKey,
                            mediaManager = mediaManager,
                            onDelete = { mediaToDelete = entry },
                            onEdit = { if (isCreatorMode) editingMedia = entry },
                            onToggleInfo = { expandedMediaId = if (isExpanded) null else entry.id },
                            onPlay = {
                                android.util.Log.d("MediaSupportDiag", "Clic Voir - ID: ${entry.id}, Title: ${entry.aiSummary}, Type: ${entry.type}")
                                if (entry.mediaUrl?.startsWith("http") == true) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(entry.mediaUrl!!))
                                        context.startActivity(intent)
                                    } catch(_: Exception) { onNavigateToDetail(entry.id) }
                                } else { onNavigateToDetail(entry.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            containerColor = theme.backgroundColor,
            title = { Text("Supprimer cette vidéo ?", color = theme.contentColor) },
            text = { Text("Cette action est irréversible.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(onClick = { viewModel.deleteMediaEntry(mediaToDelete!!); mediaToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Error)) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { mediaToDelete = null }) { Text("Annuler", color = theme.contentColor) } }
        )
    }

    if (showAddDialog) {
        com.example.phoenx.ui.components.DirectMediaDialog(
            type = "YOUTUBE",
            recipients = recipients,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, url, ids, visibility ->
                viewModel.addStandaloneMedia(title, url, "YOUTUBE", ids, desc, null, visibility)
                showAddDialog = false
            }
        )
    }

    if (editingMedia != null) {
        val isComplement = editingMedia!!.parentEntryId != null
        val type = if (isComplement) "VIDEO" else "YOUTUBE"

        com.example.phoenx.ui.components.DirectMediaDialog(
            type = type,
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
fun VHSCard(
    entry: PhoenXEntry, 
    theme: AppThemeState, 
    isCreatorMode: Boolean,
    isExpanded: Boolean,
    heirKey: ByteArray?,
    mediaManager: MediaManager,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleInfo: () -> Unit,
    onPlay: () -> Unit
) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(1.6f) // Ratio VHS plus large
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A2A))
                .clickable { onPlay() }
                .phoenXMatiere(),
            contentAlignment = Alignment.Center
        ) {
            // MINIATURE (v9.4.27 : Cover First Strategy)
            val thumbnailPath = entry.coverUrl ?: entry.localCoverPath ?: entry.localMediaPath
            
            if (thumbnailPath != null) {
                SecureAsyncImage(
                    mediaUrl = if (thumbnailPath.startsWith("http") || thumbnailPath.startsWith("users/")) thumbnailPath else null,
                    localPath = if (thumbnailPath.startsWith("/")) thumbnailPath else null,
                    explicitKey = heirKey,
                    mediaManager = mediaManager,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // PLACEHOLDER VHS DYNAMIQUE
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
                    Icon(
                        Icons.Default.Videocam, 
                        null, 
                        tint = accent.copy(alpha = 0.3f), 
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )
                }
            }

            // Étiquette VHS (Braise)
            Surface(
                modifier = Modifier.fillMaxWidth(0.85f).height(32.dp).align(Alignment.BottomCenter).padding(bottom = 6.dp),
                color = theme.contentColor.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, null, tint = accent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        entry.aiSummary.ifEmpty { "Vidéo" }, 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), 
                        color = Color.Black, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // OVERLAY UNIFIÉ (Icônes discrètes)
            
            // 1. INFO (Haut Gauche)
            if (!entry.userComment.isNullOrBlank()) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    IconButton(onClick = onToggleInfo, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                    }
                }
            }

            // 2. SUPPRIMER (Haut Droite)
            if (isCreatorMode) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                    }
                }
            }

            // 3. ÉDITER (Bas GAUCHE)
            if (isCreatorMode) {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                    }
                }
            }

            // INDICATEUR PLAY CENTRAL
            Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // TITRE SOUS LE BLOC
        Text(
            text = entry.aiSummary.ifEmpty { "Vidéo" },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // ACCORDÉON DE COMMENTAIRE
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = entry.userComment ?: "",
                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                color = theme.contentColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
