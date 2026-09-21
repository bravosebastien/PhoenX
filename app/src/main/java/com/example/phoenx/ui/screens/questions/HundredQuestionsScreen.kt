package com.example.phoenx.ui.screens.questions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.RecipientEntity

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
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    onAnswerQuestion: (String, String) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    navController: androidx.navigation.NavController,
    viewModel: HundredQuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val isRecipientMode = creatorId != null && creatorId != com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    val answeredCount = if (isRecipientMode) uiState.lockedQuestions.count { q -> uiState.unlockedQuestionId == q.id || q.unlockedAt != null } else uiState.answeredQuestionIds.size
    val totalCount = if (isRecipientMode) uiState.lockedQuestions.size else 120

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<OfflineEntry?>(null) }
    var showGuessDialog by remember { mutableStateOf(false) }
    var guessAnswer by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    LaunchedEffect(creatorId) {
        android.util.Log.e("PHOENX_DEBUG", "HundredQuestionsScreen : creatorId=$creatorId, isRecipientMode=$isRecipientMode, currentUid=${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid}")
        if (isRecipientMode) {
            viewModel.loadRecipientData(creatorId!!)
        }
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
                                Text(
                                    text = if (isRecipientMode) "Les 100 Questions" else stringResource(R.string.questions_title), 
                                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), 
                                    color = theme.contentColor
                                )
                                Text(
                                    text = if (isRecipientMode) "Résolvez les énigmes de ${uiState.creatorName}" else if (answeredCount == totalCount) 
                                        stringResource(R.string.questions_progress_all_answered) 
                                    else 
                                        stringResource(R.string.questions_progress_count, answeredCount, totalCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (answeredCount == totalCount && !isRecipientMode) Success else theme.contentColor.copy(alpha = 0.5f)
                                )
                            }
                            if (!isRecipientMode) {
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
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
                        }
                    },
                    actions = {
                        if (isRecipientMode || uiState.selectedCategory == "Mes Questions") {
                            IconButton(onClick = onNavigateToLeaderboard) {
                                Icon(Icons.Default.Leaderboard, null, tint = accent)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
                )
                
                if (!isRecipientMode) {
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
            }
        },
        floatingActionButton = {
            if (!isRecipientMode && uiState.selectedCategory == "Mes Questions") {
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
                contentPadding = PaddingValues(bottom = 100.dp, top = 24.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRecipientMode) {
                    item {
                        Text(
                            "Votre proche a préparé des énigmes pour vous. Trouvez les réponses pour débloquer ces moments partagés.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = theme.fontFamily),
                            color = theme.contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(uiState.lockedQuestions) { entry ->
                        val isUnlocked = uiState.unlockedQuestionId == entry.id || entry.unlockedAt != null
                        
                        LockedQuestionCard(
                            entry = entry,
                            isUnlocked = isUnlocked,
                            theme = theme,
                            onClick = {
                                if (isUnlocked) {
                                    navController.navigate("recipient_memory_detail/${entry.id}/${creatorId}")
                                } else {
                                    selectedEntry = entry
                                    showGuessDialog = true
                                }
                            }
                        )
                    }
                } else if (uiState.selectedCategory == "Mes Questions") {
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
            onSave = { q, h, a, vis, rids, type, wc ->
                viewModel.saveCustomQuestion(selectedEntry?.id, q, h, a, vis, rids, type, wc)
                showCreateDialog = false
                selectedEntry = null
            },
            theme = theme
        )
    }

    if (showGuessDialog && selectedEntry != null) {
        AlertDialog(
            onDismissRequest = { showGuessDialog = false; guessAnswer = ""; viewModel.clearError() },
            containerColor = theme.backgroundColor,
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Fingerprint, null, tint = accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Énigme personnelle", 
                        color = theme.contentColor,
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(selectedEntry!!.enigmaQuestion ?: "", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily), color = theme.contentColor)
                    
                    // AFFICHAGE DE L'INDICE
                    val attemptCount = uiState.attempts[selectedEntry!!.id] ?: 0
                    if (attemptCount >= 3 && !selectedEntry!!.enigmaHint.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.detective_hint_label, selectedEntry!!.enigmaHint ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.contentColor
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = guessAnswer,
                        onValueChange = { guessAnswer = it },
                        label = { Text(stringResource(R.string.detective_answer_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.error != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                            focusedTextColor = theme.contentColor,
                            unfocusedTextColor = theme.contentColor
                        )
                    )
                    if (uiState.error != null) {
                        Text(uiState.error!!, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.attemptUnlock(selectedEntry!!, guessAnswer, creatorId ?: "") },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(stringResource(R.string.detective_verify_button), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LaunchedEffect(uiState.unlockedQuestionId) {
        if (uiState.unlockedQuestionId != null) {
            showGuessDialog = false
            guessAnswer = ""
            selectedEntry = null
        }
    }
}

@Composable
fun LockedQuestionCard(
    entry: OfflineEntry, 
    isUnlocked: Boolean,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val accent = theme.accentColor

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).phoenXMatiere(),
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp, 
            if (isUnlocked) Success.copy(alpha = 0.3f) else accent.copy(alpha = 0.2f)
        )
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isUnlocked) Success else accent
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = if (isUnlocked) "QUESTION RÉPONDUE" else "QUESTION SCELLÉE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isUnlocked) Success else accent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isUnlocked) entry.enigmaQuestion ?: "" else "Déchiffre l'énigme pour voir la question",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = theme.fontFamily),
                    color = theme.contentColor
                )
            }
        }
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
    onSave: (String, String?, String, String, String, String, Int?) -> Unit,
    theme: AppThemeState,
    viewModel: HundredQuestionsViewModel = hiltViewModel()
) {
    val recipients by viewModel.recipients.collectAsState()
    var question by remember { mutableStateOf(entry?.enigmaQuestion ?: "") }
    var hint by remember { mutableStateOf(entry?.enigmaHint ?: "") }
    var answer by remember { mutableStateOf(entry?.enigmaAnswerPlain ?: "") }
    var answerType by remember { mutableStateOf(entry?.answerType ?: "WORD") }
    var expectedWordCount by remember { mutableStateOf(entry?.expectedWordCount?.toString() ?: "1") }

    var visibility by remember { mutableStateOf(entry?.visibility ?: "EVERYONE") }
    val initialSelectedIds = remember(entry, recipients) { 
        entry?.recipientIds?.split(",")?.filter { it.isNotBlank() }?.map { it.trim() }?.map { persistentId ->
            recipients.find { it.linkedUid == persistentId }?.id ?: persistentId
        }?.distinct() ?: emptyList()
    }
    val selectedRecipientIds = remember { mutableStateListOf<String>().apply { addAll(initialSelectedIds) } }

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

                // v12.7.6 : Type de réponse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = answerType == "WORD",
                        onClick = { answerType = "WORD" },
                        label = { Text("Texte") }
                    )
                    FilterChip(
                        selected = answerType == "NUMBER",
                        onClick = { answerType = "NUMBER" },
                        label = { Text("Nombre") }
                    )
                }

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text(stringResource(R.string.questions_custom_field_answer)) },
                    placeholder = { Text(if (entry != null) stringResource(R.string.questions_custom_field_answer_placeholder_edit) else stringResource(R.string.questions_custom_field_answer_placeholder_new)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (answerType == "NUMBER") androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text
                    )
                )

                if (answerType == "WORD") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mots attendus :", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            value = expectedWordCount,
                            onValueChange = { if (it.length <= 2) expectedWordCount = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.width(60.dp),
                            textStyle = TextStyle(fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Text(
                        "Rester simple facilite la tâche de votre proche.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontStyle = FontStyle.Italic),
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )
                }

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text(stringResource(R.string.questions_custom_field_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("DESTINATAIRES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                RecipientSelector(
                    recipients = recipients,
                    selectedIds = selectedRecipientIds,
                    onToggleRecipient = { id ->
                        if (selectedRecipientIds.contains(id)) selectedRecipientIds.remove(id)
                        else selectedRecipientIds.add(id)
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
                    onSave(
                        question, 
                        hint.ifBlank { null }, 
                        answer, 
                        visibility,
                        selectedRecipientIds.joinToString(","),
                        answerType, 
                        expectedWordCount.toIntOrNull()
                    ) 
                },
                enabled = question.isNotBlank() && (entry != null || answer.isNotBlank()),
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
