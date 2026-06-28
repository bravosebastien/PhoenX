package com.example.phoenx.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedQuestion by remember { mutableStateOf<Question?>(null) }
    var answerText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Enfance") }

    val filteredQuestions = remember(selectedCategory) {
        QuestionsData.allQuestions.filter { it.category == selectedCategory }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            selectedQuestion = null
            answerText = ""
            viewModel.resetSuccess()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Les 100 Questions", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(BackgroundSecondary, BackgroundPrimary), radius = 2000f)
        )) {
            if (selectedQuestion == null) {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Onglets de catégories
                    ScrollableTabRow(
                        selectedTabIndex = QuestionsData.categories.indexOf(selectedCategory),
                        containerColor = Color.Transparent,
                        contentColor = AccentPrimary,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[QuestionsData.categories.indexOf(selectedCategory)]),
                                color = AccentPrimary
                            )
                        },
                        edgePadding = 24.dp
                    ) {
                        QuestionsData.categories.forEach { category ->
                            Tab(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                text = { Text(category, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredQuestions) { question ->
                            QuestionItem(question) {
                                selectedQuestion = question
                            }
                        }
                    }
                }
            } else {
                // Saisie de la réponse
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                ) {
                    Text(
                        text = selectedQuestion!!.text,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                        color = TextPrimary,
                        lineHeight = 32.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        placeholder = { Text("Raconte ici...", color = TextTertiary, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                            focusedContainerColor = SurfaceCard.copy(alpha = 0.2f),
                            unfocusedContainerColor = SurfaceCard.copy(alpha = 0.1f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { selectedQuestion = null }) {
                            Text("Annuler", color = TextSecondary)
                        }
                        Button(
                            onClick = { viewModel.saveAnswer(selectedQuestion!!.text, answerText) },
                            enabled = answerText.isNotEmpty() && !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BackgroundPrimary)
                            } else {
                                Text("Déposer ma réponse", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionItem(question: Question, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SurfaceCard.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(androidx.compose.material.icons.Icons.Default.HelpOutline, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = question.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextTertiary)
        }
    }
}
