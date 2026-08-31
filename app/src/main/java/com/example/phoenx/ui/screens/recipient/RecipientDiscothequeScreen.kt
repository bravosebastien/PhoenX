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
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.screens.library.components.LibraryOnboardingData
import com.example.phoenx.ui.theme.*
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientDiscothequeScreen(
    creatorId: String? = null,
    filterRecipientId: String? = null, // v9.4.27
    onNavigateBack: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (com.example.phoenx.domain.model.PhoenXEntry) -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel(),
    standaloneViewModel: com.example.phoenx.ui.screens.library.LiteraryLibraryViewModel = hiltViewModel(),
    themeViewModel: com.example.phoenx.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val entries by viewModel.discothequeEntries.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val discothequeFilter by viewModel.discothequeFilter.collectAsState()
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
    var expandedMediaId by remember { mutableStateOf<String?>(null) } // v9.4.27

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

    LaunchedEffect(creatorId, filterRecipientId) {
        viewModel.setTargetCreator(creatorId)
        if (filterRecipientId != null) {
            viewModel.setViewMode(MediaViewMode.BY_RECIPIENT)
            viewModel.setFilterRecipient(filterRecipientId)
        }
    }

    if (isCreatorMode) {
        com.example.phoenx.ui.components.OnboardingPopup(
            pageKey = "discotheque",
            title = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getTitle("DISCO"),
            contentPoints = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getContent("DISCO"),
            preferenceManager = themeViewModel.preferenceManager
        )
    }

    val filteredEntries = remember(entries, viewMode, discothequeFilter, filterRecipientId) {
        val typeFiltered = when (discothequeFilter) {
            DiscothequeFilter.VOCALS -> entries.filter { it.mediaProvider == "PHOENX" || it.mediaProvider == "AUDIO" }
            DiscothequeFilter.MUSIC -> entries.filter { it.mediaProvider == "SPOTIFY" || it.mediaProvider == "DEEZER" }
            else -> entries
        }

        when (viewMode) {
            MediaViewMode.BY_RECIPIENT -> {
                if (filterRecipientId == null) typeFiltered
                else typeFiltered.filter { it.visibility == "EVERYONE" || it.recipientIds.contains(filterRecipientId!!) }
            }
            else -> typeFiltered
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
                    title = { Text("Grande Discothèque", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp), color = theme.contentColor) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        if (isCreatorMode) {
                            InfoButton(
                                title = LibraryOnboardingData.getTitle("DISCO"),
                                points = LibraryOnboardingData.getContent("DISCO")
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
                        accent = accent,
                        currentContentFilter = discothequeFilter,
                        onContentFilterChange = { viewModel.setDiscothequeFilter(it) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Le tourne-disque est silencieux...", color = theme.contentColor.copy(alpha = 0.4f))
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
                                val title = if (parentId == "standalone") "Médias isolés" else parentTitles[parentId] ?: "Souvenir"
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
                                VinylItem(
                                    entry = entry,
                                    theme = theme,
                                    isCreatorMode = isCreatorMode,
                                    isExpanded = isExpanded,
                                    heirKey = heirKey,
                                    mediaManager = mediaManager,
                                    creatorId = creatorId, // v9.4.27
                                    onDelete = { mediaToDelete = entry },
                                    onEdit = { if (isCreatorMode) editingMedia = entry },
                                    onToggleInfo = { expandedMediaId = if (isExpanded) null else entry.id },
                                    onPlay = {
                                        if (entry.mediaUrl?.startsWith("http") == true) {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(entry.mediaUrl!!))
                                                context.startActivity(intent)
                                            } catch(_: Exception) { onNavigateToDetail(entry) }
                                        } else { onNavigateToDetail(entry) }
                                    }
                                )
                            }
                        }
                    } else {
                        items(filteredEntries) { entry ->
                            val isExpanded = expandedMediaId == entry.id
                            VinylItem(
                                entry = entry,
                                theme = theme,
                                isCreatorMode = isCreatorMode,
                                isExpanded = isExpanded,
                                heirKey = heirKey,
                                mediaManager = mediaManager,
                                creatorId = creatorId, // v9.4.27
                                onDelete = { mediaToDelete = entry },
                                onEdit = { if (isCreatorMode) editingMedia = entry },
                                onToggleInfo = { expandedMediaId = if (isExpanded) null else entry.id },
                                onPlay = {
                                    if (entry.mediaUrl?.startsWith("http") == true) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(entry.mediaUrl!!))
                                            context.startActivity(intent)
                                        } catch(_: Exception) { onNavigateToDetail(entry) }
                                    } else { onNavigateToDetail(entry) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGUES DE PERSISTANCE (INCHANGÉS) ---
    if (mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            containerColor = theme.backgroundColor,
            title = { Text("Supprimer ce morceau ?", color = theme.contentColor) },
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
                    Text("1. Ouvre l'app Spotify ou Deezer.", color = theme.contentColor.copy(alpha = 0.8f))
                    Text("2. Trouve la chanson que tu veux.", color = theme.contentColor.copy(alpha = 0.8f))
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
            type = "SPOTIFY",
            recipients = recipientsList,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, url, ids, visibility, autoThumb, _ ->
                // v9.4.27 : Détection automatique Deezer
                val provider = if (url.contains("deezer")) "DEEZER" else "SPOTIFY"
                viewModel.addStandaloneMedia(title, url, provider, ids, desc, null, visibility, autoThumb)
                showAddDialog = false
            },
            onFetchMetadata = { url -> viewModel.fetchExternalMetadata(url) }
        )
    }

    if (editingMedia != null) {
        val isComplement = editingMedia!!.parentEntryId != null
        val initialType = if (isComplement) "AUDIO" else (editingMedia!!.mediaProvider ?: "SPOTIFY")

        com.example.phoenx.ui.components.DirectMediaDialog(
            type = initialType,
            recipients = recipientsList,
            onDismiss = { editingMedia = null },
            onSave = { title, desc, url, ids, visibility, _, included ->
                // v9.4.27 : Détection automatique Deezer à l'édition aussi
                val finalProvider = if (!isComplement && url.contains("deezer")) "DEEZER" 
                                   else if (!isComplement) "SPOTIFY" 
                                   else initialType
                                   
                viewModel.updateMediaEntry(editingMedia!!.id, title, desc, url, ids, visibility, isComplement, included)
                // Note: updateMediaEntry updates the entity, and we might need to ensure mediaProvider is updated
                // for standalone if it changes.
                editingMedia = null
            },
            initialTitle = editingMedia!!.aiSummary,
            initialUserComment = editingMedia!!.userComment,
            initialUrl = editingMedia!!.mediaUrl ?: "",
            initialRecipientIds = editingMedia!!.recipientIds,
            initialVisibility = editingMedia!!.visibility,
            initialIncludedInBook = editingMedia!!.includedInBook,
            onChangeCover = { coverLauncher.launch("image/*") },
            onFetchMetadata = { url -> viewModel.fetchExternalMetadata(url) }
        )
    }
}

@Composable
fun VinylItem(
    entry: PhoenXEntry, 
    theme: AppThemeState, 
    isCreatorMode: Boolean,
    isExpanded: Boolean,
    heirKey: ByteArray?,
    mediaManager: MediaManager,
    creatorId: String?, // v9.4.27
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleInfo: () -> Unit,
    onPlay: () -> Unit
) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121215))
                .clickable { onPlay() }
                .phoenXMatiere(),
            contentAlignment = Alignment.Center
        ) {
            // Pochette ou Disque
            if (entry.mediaUrl != null && (entry.mediaUrl!!.contains("storage") || entry.localMediaPath != null || entry.coverUrl != null)) {
                val displayUrl = entry.coverUrl ?: entry.mediaUrl
                val displayPath = entry.localCoverPath ?: entry.localMediaPath
                
                SecureAsyncImage(
                    mediaUrl = displayUrl,
                    localPath = displayPath,
                    explicitKey = heirKey,
                    mediaManager = mediaManager,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    creatorId = creatorId,
                    docType = entry.sourceDocType,
                    docId = entry.id,
                    field = if (entry.coverUrl != null) "coverUrl" else null
                )
            } else {
                // Disque vinyle générique (v9.4.27 : Couleur selon Provider)
                val providerColor = when(entry.mediaProvider) {
                    "DEEZER" -> Color(0xFF007BFF) // Bleu Deezer
                    "SPOTIFY" -> Color(0xFF1DB954) // Vert Spotify
                    else -> accent
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(0.85f),
                    shape = CircleShape,
                    color = Color.Black,
                    border = BorderStroke(2.dp, Color.DarkGray.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = providerColor) {
                            Icon(
                                imageVector = if (entry.mediaProvider == "DEEZER") Icons.Default.LibraryMusic else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // OVERLAY UNIFIÉ (v9.4.27)
            
            // LOGO PLATEFORME (Bas Droite - v9.4.27)
            if (entry.mediaProvider == "SPOTIFY" || entry.mediaProvider == "DEEZER") {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (entry.mediaProvider == "SPOTIFY") Icons.Default.MusicNote else Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = if (entry.mediaProvider == "SPOTIFY") Color(0xFF1DB954) else Color(0xFF007BFF),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            // 1. INFO (Haut Gauche)
            if (!entry.userComment.isNullOrBlank()) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    IconButton(
                        onClick = { onToggleInfo() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                    }
                }
            }

            // 2. SUPPRIMER (Haut Droite)
            if (isCreatorMode) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                    }
                }
            }

            // 3. ÉDITER (Bas GAUCHE)
            if (isCreatorMode) {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)) {
                    IconButton(
                        onClick = { onEdit() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp).shadow(2.dp, CircleShape))
                    }
                }
            }

            // 4. INDICATEUR PLAY CENTRAL (Discret)
            Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // TITRE SOUS LE BLOC
        Text(
            text = entry.aiSummary.ifEmpty { "Souvenir vocal" },
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
