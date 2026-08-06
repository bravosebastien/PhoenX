package com.example.phoenx.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.components.OnboardingPopup
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.screens.library.components.LibraryOnboardingData
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.ThemeViewModel
import com.example.phoenx.data.model.StandaloneMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteraryLibraryScreen(
    navController: NavController,
    isCreatorMode: Boolean = true,
    targetCreatorId: String? = null,
    viewModel: LiteraryLibraryViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val excerpts by viewModel.excerpts.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showInfoPopup by remember { mutableStateOf(false) }
    var editingExcerpt by remember { mutableStateOf<StandaloneMedia?>(null) }
    var excerptToDelete by remember { mutableStateOf<StandaloneMedia?>(null) }
    var readingExcerpt by remember { mutableStateOf<StandaloneMedia?>(null) }
    var expandedMediaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetCreatorId) {
        viewModel.setTargetCreator(targetCreatorId)
    }

    if (isCreatorMode) {
        OnboardingPopup(
            pageKey = "literary_library",
            title = LibraryOnboardingData.getTitle("LITERARY"),
            contentPoints = LibraryOnboardingData.getContent("LITERARY"),
            preferenceManager = themeViewModel.preferenceManager
        )
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(com.example.phoenx.ui.theme.LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Bibliothèque Littéraire", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            if (excerpts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Les rayons sont vides...", color = theme.contentColor.copy(alpha = 0.4f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(excerpts) { excerpt ->
                        val isExpanded = expandedMediaId == excerpt.id
                        ManuscriptItem(
                            excerpt = excerpt,
                            theme = theme,
                            isCreatorMode = isCreatorMode,
                            isExpanded = isExpanded,
                            onDelete = { excerptToDelete = excerpt },
                            onEdit = { editingExcerpt = excerpt },
                            onToggleInfo = { expandedMediaId = if (isExpanded) null else excerpt.id },
                            onClick = { readingExcerpt = excerpt }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGUES ---

    if (showAddDialog || editingExcerpt != null) {
        AddExcerptDialog(
            onDismiss = { 
                showAddDialog = false
                editingExcerpt = null
            },
            onSave = { title, userComment, content, recipients ->
                viewModel.addExcerpt(title, content, recipients, userComment, editingExcerpt?.id)
                showAddDialog = false
                editingExcerpt = null
            },
            viewModel = viewModel,
            initialExcerpt = editingExcerpt
        )
    }

    if (excerptToDelete != null) {
        AlertDialog(
            onDismissRequest = { excerptToDelete = null },
            containerColor = theme.backgroundColor,
            title = { Text("Supprimer l'extrait ?", color = theme.contentColor) },
            text = { Text("Cette action est irréversible.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(onClick = { viewModel.deleteExcerpt(excerptToDelete!!); excerptToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = com.example.phoenx.ui.theme.Error)) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { excerptToDelete = null }) { Text("Annuler", color = theme.contentColor) } }
        )
    }

    if (readingExcerpt != null) {
        AlertDialog(
            onDismissRequest = { readingExcerpt = null },
            containerColor = theme.backgroundColor,
            title = { Text(readingExcerpt!!.title.ifEmpty { "Extrait Littéraire" }, color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = readingExcerpt!!.content,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 28.sp),
                        color = theme.contentColor.copy(alpha = 0.9f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { readingExcerpt = null }) { Text("Fermer") }
            }
        )
    }
}

@Composable
fun ManuscriptItem(
    excerpt: StandaloneMedia, 
    theme: com.example.phoenx.ui.theme.AppThemeState,
    isCreatorMode: Boolean,
    isExpanded: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleInfo: () -> Unit,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(0.75f) // Format livre
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F2E9)) // Couleur papier/parchemin
                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // FOND TEXTURÉ + TEXTE STYLISÉ (Aperçu)
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = excerpt.content.take(80) + "...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    ),
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // OVERLAY UNIFIÉ (Icônes discrètes)
            
            // 1. INFO (Haut Gauche)
            if (!excerpt.userComment.isNullOrBlank()) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    IconButton(onClick = onToggleInfo, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.DarkGray, modifier = Modifier.size(18.dp).shadow(1.dp, CircleShape))
                    }
                }
            }

            // 2. SUPPRIMER (Haut Droite)
            if (isCreatorMode) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.DarkGray.copy(alpha = 0.7f), modifier = Modifier.size(18.dp).shadow(1.dp, CircleShape))
                    }
                }
            }

            // 3. ÉDITER (Bas GAUCHE)
            if (isCreatorMode) {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color.DarkGray, modifier = Modifier.size(18.dp).shadow(1.dp, CircleShape))
                    }
                }
            }

            // 4. INDICATEUR LECTURE CENTRAL (Discret)
            Icon(Icons.Default.MenuBook, null, tint = Color.Black.copy(alpha = 0.1f), modifier = Modifier.size(32.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // TITRE SOUS LE BLOC
        Text(
            text = excerpt.title.ifEmpty { "Extrait" },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // ACCORDÉON DE COMMENTAIRE
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = excerpt.userComment ?: "",
                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                color = theme.contentColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AddExcerptDialog(
    onDismiss: () -> Unit,
    onSave: (String, String?, String, List<String>) -> Unit,
    viewModel: LiteraryLibraryViewModel,
    initialExcerpt: StandaloneMedia? = null
) {
    val theme = LocalAppTheme.current
    var title by remember { mutableStateOf(initialExcerpt?.title ?: "") }
    var userComment by remember { mutableStateOf(initialExcerpt?.userComment ?: "") }
    var content by remember { mutableStateOf(initialExcerpt?.content ?: "") }
    val recipients by viewModel.recipients.collectAsState()
    val selectedIds = remember { mutableStateListOf<String>().apply { 
        initialExcerpt?.recipientIds?.let { addAll(it) }
    } }
    var visibility by remember { mutableStateOf(if (selectedIds.isEmpty()) "EVERYONE" else "RESTRICTED") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text(if (initialExcerpt == null) "Déposer un extrait" else "Modifier l'extrait", color = theme.contentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'ouvrage") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = userComment,
                    onValueChange = { userComment = it },
                    label = { Text("Commentaire personnel (optionnel)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Texte de l'extrait") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                )
                
                Text("Visibilité & Destinataires", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                RecipientSelector(
                    recipients = recipients,
                    selectedIds = selectedIds.toList(),
                    onToggleRecipient = { id ->
                        if (selectedIds.contains(id)) selectedIds.remove(id)
                        else selectedIds.add(id)
                    },
                    visibility = visibility,
                    onVisibilityChange = { visibility = it },
                    accent = theme.accentColor
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, userComment.ifBlank { null }, content, selectedIds.toList()) },
                enabled = content.isNotBlank() && title.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
