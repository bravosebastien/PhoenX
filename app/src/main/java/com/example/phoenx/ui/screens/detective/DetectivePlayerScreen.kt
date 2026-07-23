package com.example.phoenx.ui.screens.detective

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var selectedEntry by remember { mutableStateOf<OfflineEntry?>(null) }
    var answer by remember { mutableStateOf("") }

    LaunchedEffect(creatorId) {
        viewModel.loadData(creatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mode Détective", 
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.contentColor
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
            } else if (uiState.lockedEntries.isEmpty()) {
                EmptyDetectiveContent(theme)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Déchiffre les énigmes pour accéder aux souvenirs.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily),
                            color = theme.contentColor.copy(alpha = 0.7f)
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
                            theme = theme,
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
                containerColor = theme.backgroundColor,
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedEntry!!.isUltimateSecret) Icons.Default.Verified else Icons.Default.Fingerprint, 
                            null, 
                            tint = accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selectedEntry!!.isUltimateSecret) "Le Secret Ultime" else "Énigme Personnelle", 
                            color = theme.contentColor,
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (selectedEntry!!.isUltimateSecret) {
                            Text(
                                "Cette confidence a été marquée comme capitale. Aucune ouverture automatique n'est possible. Seule la réponse exacte lèvera le sceau.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = accent
                            )
                        }

                        Text(selectedEntry!!.enigmaQuestion ?: "", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily), color = theme.contentColor)
                        
                        // AFFICHAGE DE L'INDICE
                        val attemptCount = uiState.attempts[selectedEntry!!.id] ?: 0
                        if (attemptCount >= 3 && !selectedEntry!!.enigmaHint.isNullOrBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HelpOutline, null, tint = accent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Indice : ${selectedEntry!!.enigmaHint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = theme.contentColor
                                    )
                                }
                            }
                        }

                        // BANDEAU DÉLAI DE GRÂCE (Uniquement si pas Secret Ultime)
                        if (!selectedEntry!!.isUltimateSecret) {
                            val autoDays = selectedEntry!!.enigmaAutoUnlockDays ?: selectedEntry!!.unlockAfterDays
                            val daysLeft = autoDays - uiState.daysSinceActivation
                            if (daysLeft > 0) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Si tu ne connais pas la réponse, cette énigme s'ouvrira automatiquement dans $daysLeft jours.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = theme.contentColor.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${uiState.creatorName} a prévu cette option pour que tu puisses accéder à son message quoi qu'il arrive.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = theme.contentColor.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("Ta réponse") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.error != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f),
                                focusedTextColor = theme.contentColor,
                                unfocusedTextColor = theme.contentColor
                            )
                        )
                        if (uiState.error != null) {
                            Text(uiState.error!!, color = Error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.attemptUnlock(selectedEntry!!, answer) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Vérifier", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
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
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    val isUltimate = entry.isUltimateSecret

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).phoenXMatiere(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUltimate && !isUnlocked) accent.copy(alpha = 0.08f) else theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp, 
            if (isUnlocked) Success.copy(alpha = 0.3f) 
            else if (isUltimate) accent.copy(alpha = 0.5f) 
            else accent.copy(alpha = 0.2f)
        )
    ) {
        Column {
            if (!isUnlocked && !isUltimate && daysRemaining > 0) {
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "S'ouvrira automatiquement dans $daysRemaining jours",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            } else if (!isUnlocked && isUltimate) {
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, tint = accent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SECRET ULTIME SCELLÉ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.LockOpen 
                                 else if (isUltimate) Icons.Default.Verified
                                 else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) Success else accent
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = if (isUnlocked) "SOUVENIR RÉVÉLÉ" else if (isUltimate) "CONFIDENCE SACRÉE" else "CONTENU SCELLÉ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isUnlocked) Success else accent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isUnlocked) entry.aiSummary else if (isUltimate) "Réponds à l'unique question..." else "Résous l'énigme pour lire...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = theme.fontFamily),
                        color = theme.contentColor
                    )
                    
                    if (isUnlocked && isAutoUnlocked) {
                        Text(
                            text = "Cette énigme s'est ouverte avec le temps.",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = theme.contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (!entry.fallbackAnswer.isNullOrEmpty()) {
                            Text(
                                text = "Note de $creatorName : \"${entry.fallbackAnswer}\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic),
                                color = theme.contentColor,
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
fun EmptyDetectiveContent(theme: AppThemeState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(64.dp), tint = theme.contentColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Aucun mystère à résoudre.", style = MaterialTheme.typography.bodyLarge, color = theme.contentColor.copy(alpha = 0.4f))
    }
}
