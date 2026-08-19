package com.example.phoenx.ui.screens.library

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
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.OnboardingPopup
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.screens.library.components.LibraryOnboardingData
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.ThemeViewModel
import com.example.phoenx.data.model.StandaloneMedia
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale

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
    val viewMode by viewModel.viewMode.collectAsState()
    val filterRecipientId by viewModel.filterRecipientId.collectAsState()
    val recipientsList by viewModel.recipients.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val heirKey by viewModel.heirKey.collectAsState()
    
    val mediaManager = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
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

    val filteredExcerpts = remember(excerpts, viewMode, filterRecipientId) {
        when (viewMode) {
            com.example.phoenx.ui.screens.recipient.MediaViewMode.BY_RECIPIENT -> {
                if (filterRecipientId == null) excerpts
                else excerpts.filter { it.recipientIds.contains(filterRecipientId) }
            }
            else -> excerpts
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(com.example.phoenx.ui.theme.LocalBackgroundBrush.current),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Bibliothèque Littéraire", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp), color = theme.contentColor) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        if (isCreatorMode) {
                            InfoButton(
                                title = LibraryOnboardingData.getTitle("LITERARY"),
                                points = LibraryOnboardingData.getContent("LITERARY")
                            )
                            IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null, tint = accent) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                // SÉLECTEUR DE MODE DE TRI (v9.4.27 : Créateur Uniquement)
                if (isCreatorMode) {
                    com.example.phoenx.ui.screens.recipient.MediaViewModeSelector(
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
            if (filteredExcerpts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (filterRecipientId != null) "Aucun extrait pour ce destinataire." else "Les rayons sont vides...", color = theme.contentColor.copy(alpha = 0.4f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (viewMode == com.example.phoenx.ui.screens.recipient.MediaViewMode.BY_MEMORY) {
                        // En-tête unique car Standalone
                        item(span = { GridItemSpan(2) }) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                                color = accent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Extraits isolés",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(filteredExcerpts) { excerpt ->
                            val isExpanded = expandedMediaId == excerpt.id
                            ManuscriptItem(
                                excerpt = excerpt,
                                theme = theme,
                                isCreatorMode = isCreatorMode,
                                isExpanded = isExpanded,
                                heirKey = heirKey,
                                mediaManager = mediaManager,
                                creatorId = targetCreatorId, // v9.4.27
                                onDelete = { excerptToDelete = excerpt },
                                onEdit = { editingExcerpt = excerpt },
                                onToggleInfo = { expandedMediaId = if (isExpanded) null else excerpt.id },
                                onClick = { readingExcerpt = excerpt }
                            )
                        }
                    } else {
                        items(filteredExcerpts) { excerpt ->
                            val isExpanded = expandedMediaId == excerpt.id
                            ManuscriptItem(
                                excerpt = excerpt,
                                theme = theme,
                                isCreatorMode = isCreatorMode,
                                isExpanded = isExpanded,
                                heirKey = heirKey,
                                mediaManager = mediaManager,
                                creatorId = targetCreatorId, // v9.4.27
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
    }

    // --- DIALOGUES ---

    // SÉLECTEUR DE COUVERTURE (v9.4.27)
    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null && editingExcerpt != null) {
            viewModel.updateExcerptCover(editingExcerpt!!.id, uri)
            editingExcerpt = null
        }
    }

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
            onEditCover = {
                if (editingExcerpt != null) {
                    coverLauncher.launch("image/*")
                }
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
        // LECTURE PLEIN ÉCRAN (v9.4.27)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFCF9F2) // Blanc cassé / Parchemin chaud
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { readingExcerpt = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.DarkGray)
                    }
                    Text(
                        "LECTURE", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = Color.DarkGray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // Équilibre visuel
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = readingExcerpt!!.title.ifEmpty { "Extrait Littéraire" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                    
                    if (!readingExcerpt!!.userComment.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = readingExcerpt!!.userComment!!,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = Color.DarkGray.copy(alpha = 0.7f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Black.copy(alpha = 0.1f))

                    Text(
                        text = readingExcerpt!!.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Serif,
                            lineHeight = 32.sp,
                            fontSize = 18.sp
                        ),
                        color = Color.Black.copy(alpha = 0.9f)
                    )
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun ManuscriptItem(
    excerpt: StandaloneMedia, 
    theme: com.example.phoenx.ui.theme.AppThemeState,
    isCreatorMode: Boolean,
    isExpanded: Boolean,
    heirKey: ByteArray?,
    mediaManager: MediaManager,
    creatorId: String?, // v9.4.27
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
            // FOND PERSONNALISÉ (Si coverUrl présent) v9.4.27
            if (excerpt.coverUrl != null || excerpt.localCoverPath != null) {
                SecureAsyncImage(
                    mediaUrl = excerpt.coverUrl,
                    localPath = excerpt.localCoverPath,
                    explicitKey = heirKey,
                    mediaManager = mediaManager,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    creatorId = creatorId,
                    docType = "standaloneMedia",
                    docId = excerpt.id,
                    field = "coverUrl"
                )
                // Overlay sombre pour lisibilité du titre au centre
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            } else {
                // FOND TEXTURÉ
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F2E9)))
            }

            // TITRE CENTRAL (v9.4.27 : Toujours visible, gros et lisible)
            Text(
                text = excerpt.title.ifEmpty { "Manuscrit" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = if (excerpt.coverUrl != null || excerpt.localCoverPath != null) Color.White else Color.DarkGray,
                modifier = Modifier.padding(16.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

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
    onEditCover: () -> Unit, // v9.4.27
    viewModel: LiteraryLibraryViewModel,
    initialExcerpt: StandaloneMedia? = null
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var title by remember { mutableStateOf(initialExcerpt?.title ?: "") }
    var userComment by remember { mutableStateOf(initialExcerpt?.userComment ?: "") }
    var content by remember { mutableStateOf(initialExcerpt?.content ?: "") }
    var isFullEditorOpen by remember { mutableStateOf(false) } // v9.4.27
    val recipients by viewModel.recipients.collectAsState()
    
    // v9.4.27 : Normalisation UIDs -> DocIDs pour le sélecteur
    val selectedIds = remember(initialExcerpt, recipients) {
        val docIds = initialExcerpt?.recipientIds?.map { uid ->
            recipients.find { it.linkedUid == uid }?.id ?: uid
        } ?: emptyList()
        mutableStateListOf<String>().apply { addAll(docIds) }
    }

    var visibility by remember { mutableStateOf(if (selectedIds.isEmpty()) "EVERYONE" else "RESTRICTED") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text(if (initialExcerpt == null) "Déposer un extrait" else "Modifier l'extrait", color = theme.contentColor) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'ouvrage") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // BOUTON COUVERTURE (v9.4.27)
                if (initialExcerpt != null) {
                    Button(
                        onClick = onEditCover,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.1f), contentColor = accent)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Changer la photo de couverture")
                    }
                }

                OutlinedTextField(
                    value = userComment,
                    onValueChange = { userComment = it },
                    label = { Text("Commentaire personnel (optionnel)") },
                    placeholder = { Text("Ex : Nom de l'auteur, genre de la citation, pensée personnelle...", style = MaterialTheme.typography.bodySmall.copy(color = theme.contentColor.copy(alpha = 0.4f))) },
                    modifier = Modifier.fillMaxWidth()
                )

                // TEXTE DE L'EXTRAIT (Cliquable -> Éditeur plein écran)
                Box(modifier = Modifier.fillMaxWidth().clickable { isFullEditorOpen = true }) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { },
                        label = { Text("Texte de l'extrait") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        readOnly = true,
                        enabled = false, // Désactivé pour le texte mais le Box capte le clic
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = theme.contentColor,
                            disabledBorderColor = theme.contentColor.copy(alpha = 0.2f),
                            disabledLabelColor = theme.contentColor.copy(alpha = 0.4f),
                            disabledPlaceholderColor = theme.contentColor.copy(alpha = 0.2f)
                        )
                    )
                }
                
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
                onClick = { 
                    // v9.4.27 : Conversion DocIDs -> UIDs avant sauvegarde
                    val uids = selectedIds.map { docId ->
                        recipients.find { it.id == docId }?.linkedUid ?: docId
                    }
                    onSave(title, userComment.ifBlank { null }, content, uids) 
                },
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

    // ÉDITEUR LITTÉRAIRE PLEIN ÉCRAN (v9.4.27)
    if (isFullEditorOpen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullEditorOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = theme.backgroundColor
            ) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ÉDITION DE L'EXTRAIT", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                            color = accent
                        )
                        IconButton(onClick = { isFullEditorOpen = false }) {
                            Icon(Icons.Default.Check, null, tint = accent)
                        }
                    }
                    
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        placeholder = { Text("Saisis ou colle ton extrait ici...", color = theme.contentColor.copy(alpha = 0.3f)) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Serif,
                            lineHeight = 28.sp, 
                            color = theme.contentColor
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = theme.contentColor,
                            unfocusedTextColor = theme.contentColor
                        )
                    )
                }
            }
        }
    }
}
