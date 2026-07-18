package com.example.phoenx.ui.screens.detective

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectivePlayerScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: DetectiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedEntry by remember { mutableStateOf<OfflineEntry?>(null) }
    var answer by remember { mutableStateOf("") }

    LaunchedEffect(creatorId) {
        viewModel.loadData(creatorId)
    }

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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentPrimary)
            } else if (uiState.lockedEntries.isEmpty()) {
                EmptyDetectiveContent()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                        val autoDays = entry.enigmaAutoUnlockDays ?: 0
                        val daysSinceCreation = ((System.currentTimeMillis() - entry.createdAt) / (1000 * 60 * 60 * 24)).toInt()
                        
                        val isAutoUnlocked = (entry.enigmaAutoUnlockDays != null && daysSinceCreation >= autoDays) || entry.unlockedAt != null
                        
                        if (isAutoUnlocked && entry.unlockedAt == null && creatorId != null) {
                            LaunchedEffect(entry.id) {
                                viewModel.markAsAutoUnlocked(creatorId, entry.id)
                            }
                        }

                        LockedEntryCard(
                            entry = entry,
                            isUnlocked = uiState.unlockedEntryId == entry.id || isAutoUnlocked,
                            daysRemaining = if (entry.enigmaAutoUnlockDays != null && !isAutoUnlocked) autoDays - daysSinceCreation else 0,
                            isAutoUnlocked = isAutoUnlocked,
                            creatorName = uiState.creatorName,
                            onClick = { 
                                if (uiState.unlockedEntryId != entry.id && !isAutoUnlocked) selectedEntry = entry 
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
                        
                        // AFFICHAGE DE L'INDICE (Après 3 tentatives)
                        val attemptCount = uiState.attempts[selectedEntry!!.id] ?: 0
                        if (attemptCount >= 3 && !selectedEntry!!.enigmaHint.isNullOrBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.05f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Indice : ${selectedEntry!!.enigmaHint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // BANDEAU DÉLAI DE GRÂCE
                        val autoDays = selectedEntry!!.enigmaAutoUnlockDays ?: selectedEntry!!.unlockAfterDays
                        val daysLeft = autoDays - uiState.daysSinceActivation
                        if (daysLeft > 0) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Si tu ne connais pas la réponse, cette énigme s'ouvrira automatiquement dans $daysLeft jours.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${uiState.creatorName} a prévu cette option pour que tu puisses accéder à son message quoi qu'il arrive.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }

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
fun LockedEntryCard(
    entry: OfflineEntry, 
    isUnlocked: Boolean, 
    daysRemaining: Int,
    isAutoUnlocked: Boolean,
    creatorName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnlocked) Success.copy(alpha = 0.3f) else AccentPrimary.copy(alpha = 0.2f))
    ) {
        Column {
            if (!isUnlocked && daysRemaining > 0) {
                Surface(
                    color = AccentPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "S'ouvrira automatiquement dans $daysRemaining jours",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary
                    )
                }
            }

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
                    
                    if (isUnlocked && isAutoUnlocked) {
                        Text(
                            text = "Cette énigme s'est ouverte avec le temps.",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (!entry.fallbackAnswer.isNullOrEmpty()) {
                            Text(
                                text = "Note de $creatorName : \"${entry.fallbackAnswer}\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic),
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDetectiveContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(64.dp), tint = TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Aucun mystère à résoudre.", style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
    }
}
