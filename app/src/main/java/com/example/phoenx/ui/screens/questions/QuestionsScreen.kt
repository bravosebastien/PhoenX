package com.example.phoenx.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
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
    var selectedQuestion by remember { mutableStateOf<String?>(null) }
    var answerText by remember { mutableStateOf("") }

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
                title = { Text("100 Questions", style = MaterialTheme.typography.displaySmall) },
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Raconte ton histoire, une question à la fois.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                    itemsIndexed(viewModel.questions) { index, question ->
                        QuestionItem(index + 1, question) {
                            selectedQuestion = question
                        }
                    }
                }
            } else {
                // Saisie de la réponse
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                ) {
                    Text(
                        text = selectedQuestion!!,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        lineHeight = 32.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        placeholder = { Text("Raconte ici...", color = TextTertiary) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { selectedQuestion = null }) {
                            Text("Annuler", color = TextSecondary)
                        }
                        Button(
                            onClick = { viewModel.saveAnswer(selectedQuestion!!, answerText) },
                            enabled = answerText.isNotEmpty() && !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BackgroundPrimary)
                            } else {
                                Text("Déposer ma réponse", color = BackgroundPrimary)
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
fun QuestionItem(number: Int, text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SurfaceCard.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = AccentPrimary,
                modifier = Modifier.width(32.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
