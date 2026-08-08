package com.example.phoenx.ui.screens.pact

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                title = { Text(pact?.partnerName?.let { "Miroir avec $it" } ?: "Détails du Miroir", style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (pact == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
            } else {
                val isRevealed = pact.status == "active"
                
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    // --- INDICATEURS DE PROGRESSION (v9.4.27) ---
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        color = theme.contentColor.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("ÉTAT DU MIROIR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusIndicator(isDone = pact.myStatus == "completed", label = "Ta version", accent = accent, theme = theme)
                                Spacer(Modifier.width(16.dp))
                                StatusIndicator(isDone = pact.partnerStatus == "completed", label = "Version de ${pact.partnerName}", accent = accent, theme = theme)
                            }

                            if (!isRevealed) {
                                Text(
                                    "Le reflet reste scellé tant que vous n'avez pas tous les deux terminé.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent.copy(alpha = 0.7f)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Le Miroir est révélé !", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                                }
                            }
                        }
                    }

                    // --- SECTION CONSENTEMENT LIVRE (v9.4.27) ---
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        color = if (pact.myConsentToBook) accent.copy(alpha = 0.05f) else theme.contentColor.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (pact.myConsentToBook) accent.copy(alpha = 0.3f) else theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoStories, null, tint = if (pact.myConsentToBook) accent else theme.contentColor.copy(alpha = 0.3f))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Nourrir mon Livre de Vie", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                                Text("Requiert l'accord des deux voix.", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                            }
                            Switch(
                                checked = pact.myConsentToBook,
                                onCheckedChange = { viewModel.toggleConsentToBook(pact.id, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = accent)
                            )
                        }
                    }

                    Text("REFLETS PARTAGÉS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (entries.isEmpty()) {
                        EmptyMirrorContent(accent, theme) { onNavigateToCapture(pactId, pact.partnerName) }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(entries) { entry ->
                                PactEntryCard(entry, pact.partnerName, theme, isRevealed)
                            }
                            
                            if (pact.myStatus != "completed") {
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
                                
                                item {
                                    TextButton(
                                        onClick = { viewModel.completeVersion(pact.id) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text("J'ai terminé ma version", color = theme.contentColor.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(isDone: Boolean, label: String, accent: Color, theme: AppThemeState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isDone) Success else theme.contentColor.copy(alpha = 0.2f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
    }
}

@Composable
fun EmptyMirrorContent(accent: Color, theme: AppThemeState, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = theme.contentColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Aucun reflet encore déposé.", color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.phoenXMatiere()
            ) {
                Text("Déposer ton premier reflet", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PactEntryCard(entry: OfflineEntry, partnerName: String, theme: AppThemeState, isRevealed: Boolean) {
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
                    val statusText = if (isRevealed) "Versions croisées révélées" else "Ta version scellée"
                    val statusIcon = if (isRevealed) Icons.Default.AutoAwesome else Icons.Default.CheckCircle
                    val color = if (isRevealed) theme.accentColor else Success
                    
                    Icon(statusIcon, null, tint = color, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
    }
}
