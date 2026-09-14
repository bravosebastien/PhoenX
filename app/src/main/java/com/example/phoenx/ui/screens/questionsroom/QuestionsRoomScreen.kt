package com.example.phoenx.ui.screens.questionsroom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.domain.model.PendingQuestion
import com.example.phoenx.ui.screens.questions.Question
import com.example.phoenx.ui.screens.questions.QuestionsData
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsRoomScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    onAnswerQuestion: (String, String) -> Unit,
    viewModel: QuestionsRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val answeredCount = uiState.answeredQuestionIds.size
    val totalCount = 120

    LaunchedEffect(creatorId) {
        viewModel.loadData(creatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.questions_room_title), style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                            Text(
                                text = if (answeredCount == totalCount) 
                                    stringResource(R.string.questions_room_all_answered) 
                                else 
                                    stringResource(R.string.questions_room_progress_label, answeredCount, totalCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (answeredCount == totalCount) Success else theme.contentColor.copy(alpha = 0.5f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                
                ScrollableTabRow(
                    selectedTabIndex = QuestionsData.categories.indexOf(uiState.selectedCategory),
                    containerColor = Color.Transparent,
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
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.myPendingQuestions.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.questions_room_my_questions_header),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    items(uiState.myPendingQuestions) { pending ->
                        MyQuestionResultCard(pending, uiState.creatorName, theme)
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.questions_room_general_questions_header),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                items(uiState.questions) { question ->
                    QuestionCard(
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

@Composable
fun getCategoryDisplayName(category: String): String {
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

@Composable
fun QuestionCard(
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
        border = BorderStroke(
            1.dp, 
            if (isAnswered) Success.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f)
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
                            Text(stringResource(R.string.questions_room_status_answered), color = Success, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
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
            
            if (isAnswered) {
                TextButton(
                    onClick = onAnswerClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.questions_room_button_edit_answer), color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                }
            } else {
                OutlinedButton(
                    onClick = { onAnswerClick() },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Text(stringResource(R.string.questions_room_button_answer))
                }
            }
        }
    }
}

@Composable
fun MyQuestionResultCard(pending: PendingQuestion, creatorName: String, theme: AppThemeState) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = pending.questionText,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                color = theme.contentColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            when (pending.status) {
                "answered" -> {
                    Surface(
                        color = Success.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            stringResource(R.string.questions_room_result_answered_header, creatorName),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Success
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pending.answerContent ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = theme.fontFamily),
                        color = theme.contentColor
                    )
                }
                "declined" -> {
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            stringResource(R.string.questions_room_result_declined_header),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.questions_room_result_declined_desc, creatorName),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.7f)
                    )
                    if (!pending.declineNote.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${pending.declineNote}\"",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = theme.contentColor.copy(alpha = 0.5f)
                        )
                    }
                }
                else -> {
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            stringResource(R.string.questions_room_result_pending_header),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.questions_room_result_pending_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
