package com.example.phoenx.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.components.SecureAsyncImage
import dagger.hilt.android.EntryPointAccessors
import com.example.phoenx.data.media.MediaManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HundredQuestionsScreen(
    onNavigateBack: () -> Unit,
    onAnswerQuestion: (String, String) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    viewModel: HundredQuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val answeredCount = uiState.answeredQuestionIds.size
    val totalCount = 120

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<OfflineEntry?>(null) }
    
    val context = LocalContext.current
    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            Column(modifier = Modifier.background(theme.backgroundColor)) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Les 100 Questions", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                                Text(
                                    text = if (answeredCount == totalCount) 
                                        "Toutes tes histoires sont racontées." 
                                    else 
                                        "$answeredCount / $totalCount questions racontées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (answeredCount == totalCount) Success else theme.contentColor.copy(alpha = 0.5f)
                                )
                            }
                            InfoButton(
                                title = "Les 120 Questions",
                                points = listOf(
                                    "120 questions organisées en 16 catégories pour raconter ta vie en profondeur.",
                                    "Réponds à celles qui te touchent — ignore les autres.",
                                    "Chaque réponse est un souvenir normal, classé et sécurisé.",
                                    "Un badge ✓ apparaît sur les questions auxquelles tu as déjà répondu.",
                                    "Le compteur en haut montre ta progression globale."
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
                        }
                    },
                    actions = {
                        if (uiState.selectedCategory == "Mes Questions") {
                            IconButton(onClick = onNavigateToLeaderboard) {
                                Icon(Icons.Default.Leaderboard, null, tint = accent)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
                )
                
                ScrollableTabRow(
                    selectedTabIndex = QuestionsData.categories.indexOf(uiState.selectedCategory),
                    containerColor = theme.backgroundColor,
                    contentColor = accent,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[QuestionsData.categories.indexOf(uiState.selectedCategory)]),
                            color = accent
                        )
                    },
                    divider = {}
                ) {
                    QuestionsData.categories.forEach { category ->
                        Tab(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.filterQuestions(category) },
                            text = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.selectedCategory == category) theme.contentColor else theme.contentColor.copy(alpha = 0.4f)
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.selectedCategory == "Mes Questions") {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = accent,
                    contentColor = theme.backgroundColor,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.selectedCategory == "Mes Questions") {
                    if (uiState.customQuestions.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                                Text("Aucune question personnalisée.", color = theme.contentColor.copy(alpha = 0.4f))
                            }
                        }
                    } else {
                        items(uiState.customQuestions) { entry ->
                            CustomQuestionItem(
                                entry = entry,
                                theme = theme,
                                onEdit = { selectedEntry = entry; showCreateDialog = true },
                                onDelete = { viewModel.deleteCustomQuestion(entry.id) }
                            )
                        }
                    }
                } else {
                    items(uiState.questions) { question ->
                        QuestionCreatorCard(
                            question = question,
                            isAnswered = uiState.answeredQuestionIds.contains(question.id),
                            theme = theme,
                            onAnswerClick = { onAnswerQuestion(question.id, question.text) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CustomQuestionDialog(
            entry = selectedEntry,
            onDismiss = { showCreateDialog = false; selectedEntry = null },
            onSave = { q, h, a, s, r, p ->
                viewModel.saveCustomQuestion(selectedEntry?.id, q, h, a, s, r, p)
                showCreateDialog = false
                selectedEntry = null
            },
            theme = theme
        )
    }
}

@Composable
fun QuestionCreatorCard(
    question: Question,
    isAnswered: Boolean,
    theme: AppThemeState,
    onAnswerClick: () -> Unit
) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isAnswered) Success.copy(alpha = 0.3f) else theme.contentColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = question.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                if (isAnswered) {
                    Surface(
                        color = Success.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✓ Répondu", color = Success, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = question.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                color = theme.contentColor
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (!isAnswered) {
                OutlinedButton(
                    onClick = onAnswerClick,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent)
                ) {
                    Text("Répondre")
                }
            } else {
                TextButton(
                    onClick = onAnswerClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Modifier ma réponse", color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun CustomQuestionItem(
    entry: OfflineEntry,
    theme: AppThemeState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val accent = theme.accentColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = accent.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = entry.enigmaQuestion ?: "Sans question",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    modifier = Modifier.weight(1f),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (!entry.enigmaHint.isNullOrBlank()) {
                        Text(
                            "Indice : ${entry.enigmaHint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Supprimer", tint = Error.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Modifier", tint = accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomQuestionDialog(
    entry: OfflineEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String, String, List<String>, File?) -> Unit,
    theme: AppThemeState,
    viewModel: HundredQuestionsViewModel = hiltViewModel()
) {
    var question by remember { mutableStateOf(entry?.enigmaQuestion ?: "") }
    var hint by remember { mutableStateOf(entry?.enigmaHint ?: "") }
    var answer by remember { mutableStateOf("") } // Toujours vide au départ pour la sécu
    var story by remember { mutableStateOf(entry?.let { viewModel.decryptStory(it.encryptedPayload) } ?: "") }
    var photoFile by remember { mutableStateOf<File?>(null) }
    
    val recipients by viewModel.recipients.collectAsState()
    var selectedRecipientIds by remember { 
        mutableStateOf(entry?.recipientIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) 
    }
    
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val file = File(context.cacheDir, "custom_q_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            photoFile = file
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { 
            Text(
                if (entry == null) "Nouvelle question" else "Modifier la question",
                color = theme.contentColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("La question à poser") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("La réponse attendue") },
                    placeholder = { Text(if (entry != null) "Laisser vide pour ne pas changer" else "Ex: Paris") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Indice (optionnel)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(8.dp))
                Text("LE RÉCIT À DÉBLOQUER", style = MaterialTheme.typography.labelSmall, color = theme.accentColor)
                OutlinedTextField(
                    value = story,
                    onValueChange = { story = it },
                    placeholder = { Text("Raconte ici la vraie histoire...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )

                Text("DESTINATAIRES", style = MaterialTheme.typography.labelSmall, color = theme.accentColor)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recipients.forEach { recipient ->
                        FilterChip(
                            selected = selectedRecipientIds.contains(recipient.id),
                            onClick = {
                                selectedRecipientIds = if (selectedRecipientIds.contains(recipient.id)) {
                                    selectedRecipientIds - recipient.id
                                } else {
                                    selectedRecipientIds + recipient.id
                                }
                            },
                            label = { Text(recipient.name) }
                        )
                    }
                }

                if (photoFile != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp))) {
                        AsyncImage(model = photoFile, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(onClick = { photoFile = null }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                } else if (entry?.mediaUrl != null) {
                    // Si on a déjà une image sur Firestore
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp))) {
                        SecureAsyncImage(
                            mediaUrl = entry.mediaUrl,
                            mediaManager = viewModel.mediaManager,
                            isEncrypted = false,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            docType = "entries",
                            docId = entry.id
                        )
                        Button(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.align(Alignment.Center),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                        ) {
                            Text("Changer l'image")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddAPhoto, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ajouter une photo")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(question, hint.ifBlank { null }, answer, story, selectedRecipientIds, photoFile) },
                enabled = question.isNotBlank() && story.isNotBlank() && (entry != null || answer.isNotBlank()) && selectedRecipientIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
            ) {
                Text("Enregistrer", color = theme.backgroundColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    )
}
