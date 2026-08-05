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
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.theme.*

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
                            onDelete = { mediaToDelete = entry }
                        ) { onNavigateToDetail(entry.id) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        com.example.phoenx.ui.components.DirectPhotoDialog(
            recipients = recipients,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, uri, ids ->
                // Conversion Uri -> File temporaire pour upload
                val tempFile = java.io.File(context.cacheDir, "standalone_photo_${java.util.UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.uploadAndAddStandalonePhoto(title, desc, tempFile, ids)
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
                        viewModel.deleteMediaEntry(mediaToDelete!!) // v9.4.27 : Correction type suppression
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
fun PhotoItem(
    entry: PhoenXEntry, 
    theme: AppThemeState, 
    isCreatorMode: Boolean,
    onDelete: () -> Unit,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PhotoLibrary, null, tint = theme.accentColor.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.aiSummary.ifEmpty { "Photo" },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = theme.contentColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        // Bouton Supprimer (v9.3.3)
        if (isCreatorMode) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
