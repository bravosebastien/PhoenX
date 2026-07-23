package com.example.phoenx.ui.screens.questions

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HundredQuestionsScreen(
    onNavigateBack: () -> Unit,
    onAnswerQuestion: (String, String) -> Unit,
    viewModel: HundredQuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val answeredCount = uiState.answeredQuestionIds.size
    val totalCount = 120

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
