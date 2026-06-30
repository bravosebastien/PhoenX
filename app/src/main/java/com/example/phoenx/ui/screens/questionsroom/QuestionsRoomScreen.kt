package com.example.phoenx.ui.screens.questionsroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.domain.model.PendingQuestion
import com.example.phoenx.ui.components.BookItem
import com.example.phoenx.ui.theme.*

/**
 * QuestionsRoomScreen (Signature PHOEN-X 5.0)
 * La "Salle des Questions" : les proches dialoguent avec l'héritage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsRoomScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuestionsRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    
    // Simuler des questions posées par ce destinataire
    val myQuestions = remember { mutableStateListOf<PendingQuestion>() }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Salle des Questions", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BackgroundSecondary, BackgroundPrimary)))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(24.dp)
            ) {
                // --- SECTION TES QUESTIONS ---
                if (myQuestions.isNotEmpty()) {
                    item {
                        Text("TES QUESTIONS", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(myQuestions) { question ->
                        MyQuestionCard(question)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = TextTertiary.copy(alpha = 0.1f))
                    }
                }

                item {
                    // Explication Narrative
                    Text(
                        "Posez une question pour retrouver ses mots.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "L'IA cherchera les extraits les plus fidèles parmi tout ce qu'il a légué.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Barre de recherche IA
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Ex: Que pensait-il du bonheur ?", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.askQuestion(query) }) {
                                Icon(Icons.Default.AutoAwesome, null, tint = AccentPrimary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }

                // Résultats
                if (uiState.isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentPrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Recherche dans les archives...", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else if (uiState.hasSearched && uiState.results.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun extrait ne semble correspondre directement à cette question.", color = TextTertiary, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(uiState.results) { entry ->
                        ResultSnippet(entry)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MyQuestionCard(question: PendingQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.4f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when(question.status) {
                        "answered" -> Success.copy(alpha = 0.2f)
                        "declined" -> TextTertiary.copy(alpha = 0.2f)
                        else -> AccentPrimary.copy(alpha = 0.2f)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when(question.status) {
                            "answered" -> "Répondu"
                            "declined" -> "Non répondu par choix"
                            else -> "Sans réponse"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(question.status) {
                            "answered" -> Success
                            "declined" -> TextTertiary
                            else -> AccentPrimary
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic),
                color = TextSecondary
            )

            when (question.status) {
                "answered" -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = TextTertiary.copy(alpha = 0.1f))
                    // Simuler une réponse
                    Text(
                        text = "Ma réponse scellée apparaîtra ici, avec mes propres mots, tels que je les ai déposés pour toi.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                        color = TextPrimary
                    )
                }
                "declined" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ton proche a vu cette question et a choisi de ne pas y répondre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cette question n'a pas eu de réponse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
fun ResultSnippet(entry: PhoenXEntry) {
    Column {
        Text(
            text = "À ${entry.ageAtCreation.years} ans, il écrivait :",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // On réutilise le composant de livre pour garder la cohérence "Matière"
        BookItem(entry = entry) {}
    }
}
