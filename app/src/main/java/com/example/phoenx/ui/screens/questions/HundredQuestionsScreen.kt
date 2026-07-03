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
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HundredQuestionsScreen(
    onNavigateBack: () -> Unit,
    onAnswerQuestion: (String, String) -> Unit,
    viewModel: HundredQuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val answeredCount = uiState.answeredQuestionIds.size
    val totalCount = 120

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            Column(modifier = Modifier.background(BackgroundPrimary)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Les 100 Questions", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif))
                            Text(
                                text = if (answeredCount == totalCount) 
                                    "Toutes tes histoires sont racontées." 
                                else 
                                    "$answeredCount / $totalCount questions racontées",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (answeredCount == totalCount) Success else TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
                )
                
                ScrollableTabRow(
                    selectedTabIndex = QuestionsData.categories.indexOf(uiState.selectedCategory),
                    containerColor = BackgroundPrimary,
                    contentColor = AccentPrimary,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[QuestionsData.categories.indexOf(uiState.selectedCategory)]),
                            color = AccentPrimary
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
                                    color = if (uiState.selectedCategory == category) TextPrimary else Color(0xFF5C5855)
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
                CircularProgressIndicator(color = AccentPrimary)
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
    onAnswerClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isAnswered) Success.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = question.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary,
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
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (!isAnswered) {
                OutlinedButton(
                    onClick = onAnswerClick,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary)
                ) {
                    Text("Répondre")
                }
            } else {
                TextButton(
                    onClick = onAnswerClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Modifier ma réponse", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
