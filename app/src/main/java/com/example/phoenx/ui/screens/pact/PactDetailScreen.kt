package com.example.phoenx.ui.screens.pact

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PactDetailScreen(
    pactId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: (String, String) -> Unit,
    viewModel: PactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val pact = uiState.pacts.find { it.id == pactId }
    val entries by viewModel.getEntriesForPact(pactId).collectAsState(initial = emptyList())

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text(pact?.partnerName?.let { "Pacte avec $it" } ?: "Détails du Pacte", style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* ... */ },
                containerColor = accent,
                contentColor = theme.backgroundColor
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (pact == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    Text("ÉVÉNEMENTS PARTAGÉS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (entries.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = theme.contentColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Aucun événement encore raconté.", color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onNavigateToCapture(pactId, pact.partnerName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    modifier = Modifier.phoenXMatiere()
                                ) {
                                    Text("Raconter le premier souvenir", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(entries) { entry ->
                                PactEntryCard(entry, pact.partnerName, theme)
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { onNavigateToCapture(pactId, pact.partnerName) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                                ) {
                                    Text("Ajouter une vérité", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "Rappel : Le contenu est chiffré. Vous ne verrez la version de ${pact.partnerName} qu'après activation mutuelle.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.4f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PactEntryCard(entry: OfflineEntry, partnerName: String, theme: AppThemeState) {
    Surface(
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HistoryEdu, null, tint = theme.accentColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.aiSummary.ifEmpty { "Événement sans titre" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ta version scellée", style = MaterialTheme.typography.labelSmall, color = Success)
                }
            }
        }
    }
}
