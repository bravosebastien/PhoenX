package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientDiscothequeScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel(),
    standaloneViewModel: com.example.phoenx.ui.screens.library.LiteraryLibraryViewModel = hiltViewModel(),
    themeViewModel: com.example.phoenx.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val entries by viewModel.discothequeEntries.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = androidx.compose.ui.platform.LocalContext.current
    val isCreatorMode = creatorId == null || creatorId == viewModel.currentUid
    var showAddDialog by remember { mutableStateOf(false) }
    var showInfoPopup by remember { mutableStateOf(false) }
    var editingMedia by remember { mutableStateOf<PhoenXEntry?>(null) }
    var mediaToDelete by remember { mutableStateOf<PhoenXEntry?>(null) }
    val recipients by standaloneViewModel.recipients.collectAsState()

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    if (isCreatorMode) {
        com.example.phoenx.ui.components.OnboardingPopup(
            pageKey = "discotheque",
            title = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getTitle("DISCO"),
            contentPoints = com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getContent("DISCO"),
            preferenceManager = themeViewModel.preferenceManager
        )
    }

    if (showInfoPopup) {
        AlertDialog(
            onDismissRequest = { showInfoPopup = false },
            containerColor = theme.backgroundColor,
            title = { Text(com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getTitle("DISCO"), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getContent("DISCO").forEach { point ->
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
                    title = { Text("Grande Discothèque", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Note : Ce morceau sera déposé seul. Pour l'associer à un souvenir précis, utilisez l'écran de Capture.",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Le tourne-disque est silencieux pour le moment.", color = theme.contentColor.copy(alpha = 0.4f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(entries) { entry ->
                        VinylItem(
                            entry = entry, 
                            theme = theme,
                            isCreatorMode = isCreatorMode,
                            onDelete = { mediaToDelete = entry },
                            onEdit = { if (isCreatorMode) editingMedia = entry }
                        ) { 
                            if (entry.mediaUrl != null && (entry.mediaUrl!!.startsWith("http"))) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(entry.mediaUrl!!))
                                    context.startActivity(intent)
                                } catch(e: Exception) {
                                    onNavigateToDetail(entry.id)
                                }
                            } else {
                                onNavigateToDetail(entry.id)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        com.example.phoenx.ui.components.DirectMediaDialog(
            type = "SPOTIFY",
            recipients = recipients,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, url, ids, visibility ->
                viewModel.addStandaloneMedia(title, url, "SPOTIFY", ids, desc, null, visibility)
                showAddDialog = false
            }
        )
    }

    if (editingMedia != null) {
        com.example.phoenx.ui.components.DirectMediaDialog(
            type = "SPOTIFY",
            recipients = recipients,
            onDismiss = { editingMedia = null },
            onSave = { title, desc, url, ids, visibility ->
                viewModel.addStandaloneMedia(title, url, "SPOTIFY", ids, desc, editingMedia!!.id, visibility)
                editingMedia = null
            },
            initialTitle = editingMedia!!.aiSummary,
            initialDescription = editingMedia!!.description,
            initialUrl = editingMedia!!.mediaUrl ?: "",
            initialRecipientIds = editingMedia!!.recipientIds,
            initialVisibility = editingMedia!!.visibility
        )
    }

    if (mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text("Supprimer ce morceau ?", color = theme.contentColor) },
            text = { Text("Cette action est irréversible.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStandaloneMedia(mediaToDelete!!)
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
}

@Composable
fun VinylItem(
    entry: PhoenXEntry, 
    theme: AppThemeState, 
    isCreatorMode: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFF121215)) // Pochette noire (fixe pour le style vinyle)
                .clickable(onClick = onClick)
                .phoenXMatiere(),
            contentAlignment = Alignment.Center
        ) {
            // ... (disque et play) ...
            Surface(
                modifier = Modifier.fillMaxSize(0.85f),
                shape = CircleShape,
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.DarkGray.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Étiquette centrale
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = accent
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = theme.backgroundColor, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            
            // Overlay Play
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(12.dp))
            }

            // Boutons Actions (v9.3.3)
            if (isCreatorMode && entry.parentEntryId == null) {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    // Éditer (uniquement si Standalone Spotify)
                    if (entry.mediaUrl?.contains("spotify.com") == true) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = entry.aiSummary.ifEmpty { "Souvenir vocal" },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        entry.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            text = "À ${entry.ageAtCreation.years} ans",
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.6f)
        )
    }
}
