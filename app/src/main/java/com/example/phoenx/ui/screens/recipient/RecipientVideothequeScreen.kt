package com.example.phoenx.ui.screens.recipient

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.screens.library.components.LibraryOnboardingData
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
    val viewMode by viewModel.viewMode.collectAsState()
    val filterRecipientId by viewModel.filterRecipientId.collectAsState()
    val parentTitles by viewModel.parentTitles.collectAsState()
    val recipientsList by viewModel.recipientsFlow.collectAsState()

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
    var showHowToPopup by remember { mutableStateOf(false) } // v9.4.27
    var editingMedia by remember { mutableStateOf<PhoenXEntry?>(null) }
    var mediaToDelete by remember { mutableStateOf<PhoenXEntry?>(null) }
    var expandedMediaId by remember { mutableStateOf<String?>(null) } 

    // SÉLECTEUR DE COUVERTURE (v9.4.27)
    val coverLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null && editingMedia != null) {
            val isComplement = editingMedia!!.parentEntryId != null
            viewModel.updateMediaCover(editingMedia!!.id, uri, isComplement)
            editingMedia = null
        }
    }

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    if (isCreatorMode) {
        com.example.phoenx.ui.components.OnboardingPopup(
            pageKey = "videotheque",
            title = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getTitle("VIDEO"),
            contentPoints = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getContent("VIDEO"),
            preferenceManager = themeViewModel.preferenceManager
        )
    }

    val filteredEntries = remember(entries, viewMode, filterRecipientId) {
        when (viewMode) {
            MediaViewMode.BY_RECIPIENT -> {
                if (filterRecipientId == null) entries
                else entries.filter { it.visibility == "EVERYONE" || it.recipientIds.contains(filterRecipientId!!) }
            }
            else -> entries
        }
    }

    val groupedEntries = remember(filteredEntries, viewMode) {
        if (viewMode == MediaViewMode.BY_MEMORY) {
            filteredEntries.groupBy { it.parentEntryId ?: "standalone" }
        } else emptyMap()
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Grande Vidéothèque", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp), color = theme.contentColor) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        if (isCreatorMode) {
                            InfoButton(
                                title = LibraryOnboardingData.getTitle("VIDEO"),
                                points = LibraryOnboardingData.getContent("VIDEO")
                            )
                            IconButton(onClick = { showHowToPopup = true }) { Icon(Icons.Default.Add, null, tint = accent) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                // SÉLECTEUR DE MODE DE TRI (v9.4.27 : Créateur Uniquement)
                if (isCreatorMode) {
                    MediaViewModeSelector(
                        currentMode = viewMode,
                        onModeChange = { viewModel.setViewMode(it) },
                        filterRecipientId = filterRecipientId,
                        onRecipientChange = { viewModel.setFilterRecipient(it) },
                        recipients = recipientsList,
                        theme = theme,
                        accent = accent
                    )
                }
            }
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
                    if (viewMode == MediaViewMode.BY_MEMORY) {
                        groupedEntries.forEach { (parentId, group) ->
                            item(span = { GridItemSpan(2) }) {
                                val title = if (parentId == "standalone") "Vidéos isolées" else parentTitles[parentId] ?: "Souvenir"
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                                    color = accent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(group) { entry ->
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
                                        // v9.4.27 : Navigation Intelligente (YouTube vs Local)
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
                    } else {
                        items(filteredEntries) { entry ->
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
                                    // v9.4.27 : Navigation Intelligente (YouTube vs Local)
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

    if (showHowToPopup) {
        AlertDialog(
            onDismissRequest = { showHowToPopup = false },
            containerColor = theme.backgroundColor,
            title = { Text("Comment récupérer un lien ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Ouvre l'app YouTube.", color = theme.contentColor.copy(alpha = 0.8f))
                    Text("2. Trouve la vidéo que tu veux.", color = theme.contentColor.copy(alpha = 0.8f))
                    Text("3. Appuie sur 'Partager' puis 'Copier le lien'.", color = theme.contentColor.copy(alpha = 0.8f))
                    Text("4. Reviens ici et colle-le.", color = theme.contentColor.copy(alpha = 0.8f))
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showHowToPopup = false
                    showAddDialog = true 
                }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Continuer", color = theme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHowToPopup = false }) {
                    Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
                }
            }
        )
    }

    if (showAddDialog) {
        com.example.phoenx.ui.components.DirectMediaDialog(
            type = "YOUTUBE",
            recipients = recipientsList,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, url, ids, visibility, autoThumb ->
                viewModel.addStandaloneMedia(title, url, "YOUTUBE", ids, desc, null, visibility, autoThumb)
                showAddDialog = false
            },
            onFetchMetadata = { url -> viewModel.fetchExternalMetadata(url) }
        )
    }

    if (editingMedia != null) {
        val isComplement = editingMedia!!.parentEntryId != null
        val type = if (isComplement) "VIDEO" else "YOUTUBE"

        com.example.phoenx.ui.components.DirectMediaDialog(
            type = type,
            recipients = recipientsList,
            onDismiss = { editingMedia = null },
            onSave = { title, comment, url, ids, visibility, _ ->
                viewModel.updateMediaEntry(editingMedia!!.id, title, comment, url, ids, visibility, isComplement)
                editingMedia = null
            },
            initialTitle = editingMedia!!.aiSummary,
            initialUserComment = editingMedia!!.userComment,
            initialUrl = editingMedia!!.mediaUrl ?: "",
            initialRecipientIds = editingMedia!!.recipientIds,
            initialVisibility = editingMedia!!.visibility,
            onChangeCover = { coverLauncher.launch("image/*") },
            onFetchMetadata = { url -> viewModel.fetchExternalMetadata(url) }
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
            // MINIATURE (v9.4.27 : Cover First Strategy - Corrigée)
            val hasImage = entry.coverUrl != null || entry.localCoverPath != null || entry.localMediaPath != null
            
            if (hasImage) {
                SecureAsyncImage(
                    mediaUrl = entry.coverUrl ?: entry.mediaUrl?.takeIf { !it.startsWith("/") },
                    localPath = entry.localCoverPath ?: entry.localMediaPath?.takeIf { it.startsWith("/") },
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
                    Text(
                        entry.aiSummary.ifEmpty { "Vidéo" }, 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), 
                        color = Color.Black, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // OVERLAY UNIFIÉ (Icônes discrètes)
            
            // LOGO YOUTUBE (Bas Droite - v9.4.27)
            if (entry.mediaProvider == "YOUTUBE") {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            // 1. INFO (Haut Gauche)
            if (!entry.userComment.isNullOrBlank()) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    IconButton(onClick = onToggleInfo, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                    }
                }
            }

            // 2. ACTIONS HAUT DROITE (v9.4.27 : Unification & Position Camera)
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indicateur Caméra (Discret)
                    Icon(
                        Icons.Default.Videocam, 
                        null, 
                        tint = Color.White.copy(alpha = 0.7f), 
                        modifier = Modifier.size(18.dp).shadow(1.dp, CircleShape)
                    )
                    
                    if (isCreatorMode) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                        }
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
