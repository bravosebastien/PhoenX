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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
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

@Composable
private fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "Toutes" -> stringResource(R.string.question_cat_all)
        "Enfance" -> stringResource(R.string.question_cat_childhood)
        "Famille" -> stringResource(R.string.question_cat_family)
        "Amour" -> stringResource(R.string.question_cat_love)
        "Amitié" -> stringResource(R.string.question_cat_friendship)
        "Travail" -> stringResource(R.string.question_cat_work)
        "Argent & Réussite" -> stringResource(R.string.question_cat_money)
        "Valeurs" -> stringResource(R.string.question_cat_values)
        "Foi & Spiritualité" -> stringResource(R.string.question_cat_faith)
        "Corps & Santé" -> stringResource(R.string.question_cat_health)
        "Regrets" -> stringResource(R.string.question_cat_regrets)
        "Rêves" -> stringResource(R.string.question_cat_dreams)
        "Voyages & Lieux" -> stringResource(R.string.question_cat_travel)
        "Créativité & Passions" -> stringResource(R.string.question_cat_creativity)
        "Secrets & Aveux" -> stringResource(R.string.question_cat_secrets)
        "Sagesse" -> stringResource(R.string.question_cat_wisdom)
        "Mes Questions" -> stringResource(R.string.question_cat_my_questions)
        else -> category
    }
}

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
                                Text(stringResource(R.string.questions_title), style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                                Text(
                                    text = if (answeredCount == totalCount) 
                                        stringResource(R.string.questions_progress_all_answered) 
                                    else 
                                        stringResource(R.string.questions_progress_count, answeredCount, totalCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (answeredCount == totalCount) Success else theme.contentColor.copy(alpha = 0.5f)
                                )
                            }
                            InfoButton(
                                title = stringResource(R.string.questions_info_title),
                                points = listOf(
                                    stringResource(R.string.questions_info_p1),
                                    stringResource(R.string.questions_info_p2),
                                    stringResource(R.string.questions_info_p3),
                                    stringResource(R.string.questions_info_p4),
                                    stringResource(R.string.questions_info_p5)
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
                                    text = getCategoryDisplayName(category),
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
                                Text(stringResource(R.string.questions_custom_empty), color = theme.contentColor.copy(alpha = 0.4f))
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
                    text = getCategoryDisplayName(question.category).uppercase(),
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
                            Text(stringResource(R.string.questions_status_answered), color = Success, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (question.textResId != 0) stringResource(question.textResId) else question.text,
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
                    Text(stringResource(R.string.questions_btn_answer))
                }
            } else {
                TextButton(
                    onClick = onAnswerClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.questions_btn_edit_answer), color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
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
                    text = entry.enigmaQuestion ?: stringResource(R.string.questions_custom_no_question),
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
                            stringResource(R.string.questions_custom_hint_label, entry.enigmaHint),
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
                            Icon(Icons.Default.Delete, stringResource(R.string.questions_custom_delete_desc), tint = Error.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, stringResource(R.string.questions_custom_edit_desc), tint = accent)
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
                if (entry == null) stringResource(R.string.questions_custom_dialog_title_new) else stringResource(R.string.questions_custom_dialog_title_edit),
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
                    label = { Text(stringResource(R.string.questions_custom_field_question)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text(stringResource(R.string.questions_custom_field_answer)) },
                    placeholder = { Text(if (entry != null) stringResource(R.string.questions_custom_field_answer_placeholder_edit) else stringResource(R.string.questions_custom_field_answer_placeholder_new)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text(stringResource(R.string.questions_custom_field_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.questions_custom_section_story), style = MaterialTheme.typography.labelSmall, color = theme.accentColor)
                OutlinedTextField(
                    value = story,
                    onValueChange = { story = it },
                    placeholder = { Text(stringResource(R.string.questions_custom_story_placeholder)) },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )

                Text(stringResource(R.string.questions_custom_section_recipients), style = MaterialTheme.typography.labelSmall, color = theme.accentColor)
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
                            Text(stringResource(R.string.questions_custom_btn_change_photo))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddAPhoto, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.questions_custom_btn_add_photo))
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
                Text(stringResource(R.string.questions_btn_save), color = theme.backgroundColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.questions_btn_cancel), color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    )
}
