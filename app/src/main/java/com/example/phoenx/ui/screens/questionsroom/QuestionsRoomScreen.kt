package com.example.phoenx.ui.screens.questionsroom

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.BookItem
import com.example.phoenx.ui.theme.*

/**
 * QuestionsRoomScreen (Signature PHOEN-X 5.0)
 * La "Salle des Questions" : les proches dialoguent avec l'héritage.
 * L'IA ne répond pas directement mais remonte les extraits réels pertinents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsRoomScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuestionsRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }

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
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(BackgroundSecondary, BackgroundPrimary))
        )) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                
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

                // Résultats
                if (uiState.isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Recherche dans les archives...", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else if (uiState.hasSearched && uiState.results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Aucun extrait ne semble correspondre directement à cette question.", color = TextTertiary, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 40.dp)
                    ) {
                        items(uiState.results) { entry ->
                            ResultSnippet(entry)
                        }
                    }
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
        BookItem(entry = entry, onClick = {})
    }
}
