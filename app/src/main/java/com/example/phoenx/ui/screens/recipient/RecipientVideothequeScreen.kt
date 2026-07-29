package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*

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
    val isCreatorMode = creatorId == null || creatorId == viewModel.currentUid
    var showAddDialog by remember { mutableStateOf(false) }
    var showInfoPopup by remember { mutableStateOf(false) }
    var mediaToDelete by remember { mutableStateOf<PhoenXEntry?>(null) }
    val recipients by standaloneViewModel.recipients.collectAsState()

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

    if (showInfoPopup) {
        AlertDialog(
            onDismissRequest = { showInfoPopup = false },
            containerColor = theme.backgroundColor,
            title = { Text(com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getTitle("VIDEO"), color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.phoenx.ui.screens.library.components.LibraryOnboardingData.getContent("VIDEO").forEach { point ->
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
                    title = { Text("Grande Vidéothèque", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
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
                                "Note : Cette vidéo sera déposée seule. Pour l'associer à un souvenir précis, utilisez l'écran de Capture.",
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
                    Text("Le projecteur est éteint pour le moment.", color = theme.contentColor.copy(alpha = 0.4f), textAlign = TextAlign.Center)
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
                        VHSCard(
                            entry = entry, 
                            theme = theme,
                            isCreatorMode = isCreatorMode,
                            onDelete = { mediaToDelete = entry }
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
            type = "YOUTUBE",
            recipients = recipients,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, url, ids ->
                viewModel.addStandaloneMedia(title, url, "YOUTUBE", ids, desc)
                showAddDialog = false
            }
        )
    }

    if (mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text("Supprimer cette vidéo ?", color = theme.contentColor) },
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
fun VHSCard(
    entry: PhoenXEntry, 
    theme: AppThemeState, 
    isCreatorMode: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFF1A1A2A)) // Corps VHS (fixe pour le style cassette)
                .clickable(onClick = onClick)
                .phoenXMatiere(),
            contentAlignment = Alignment.Center
        ) {
            // Fenêtre de la cassette
            Surface(
                modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.4f).align(Alignment.TopCenter).padding(top = 12.dp),
                color = Color.Black.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    repeat(2) {
                        Surface(modifier = Modifier.size(24.dp), shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFF2E2E35)) {}
                    }
                }
            }
            
            // Étiquette (Braise)
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).align(Alignment.BottomCenter).padding(bottom = 8.dp),
                color = theme.contentColor.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(entry.aiSummary.ifEmpty { "Souvenir vidéo" }, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black, maxLines = 1)
                }
            }

            // Bouton Play invisible (uniquement l'icône)
            Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))

            // Bouton Supprimer (v9.3.3)
            if (isCreatorMode && entry.parentEntryId == null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
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
