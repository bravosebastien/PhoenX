package com.example.phoenx.ui.screens.detective

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectiveScreen(
    onNavigateBack: () -> Unit,
    viewModel: DetectiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedEntry by remember { mutableStateOf<OfflineEntry?>(null) }
    var answer by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Mode Détective", style = MaterialTheme.typography.displaySmall) },
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
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentPrimary)
            } else if (uiState.lockedEntries.isEmpty()) {
                EmptyDetectiveContent(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Déchiffre les énigmes pour accéder aux souvenirs.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(uiState.lockedEntries) { entry ->
                        LockedEntryCard(
                            entry = entry,
                            isUnlocked = uiState.unlockedEntryId == entry.id,
                            onClick = { 
                                if (uiState.unlockedEntryId != entry.id) selectedEntry = entry 
                            }
                        )
                    }
                }
            }
        }

        if (selectedEntry != null) {
            AlertDialog(
                onDismissRequest = { selectedEntry = null; answer = ""; viewModel.clearError() },
                containerColor = BackgroundSecondary,
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, null, tint = AccentPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Énigme Personnelle", color = TextPrimary)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(selectedEntry!!.enigmaQuestion ?: "", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("Ta réponse") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.error != null
                        )
                        if (uiState.error != null) {
                            Text(uiState.error!!, color = Error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.attemptUnlock(selectedEntry!!, answer) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text("Vérifier", color = BackgroundPrimary)
                    }
                }
            )
        }

        // Auto-close dialog on success
        LaunchedEffect(uiState.unlockedEntryId) {
            if (uiState.unlockedEntryId != null) {
                selectedEntry = null
                answer = ""
            }
        }
    }
}

@Composable
fun LockedEntryCard(entry: OfflineEntry, isUnlocked: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnlocked) Success.copy(alpha = 0.3f) else AccentPrimary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isUnlocked) Success else AccentPrimary
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = if (isUnlocked) "SOUVENIR RÉVÉLÉ" else "CONTENU SCELLÉ",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnlocked) Success else AccentPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isUnlocked) entry.aiSummary else "Résous l'énigme pour lire...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun EmptyDetectiveContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(64.dp), tint = TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Aucun mystère à résoudre.", style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
    }
}
