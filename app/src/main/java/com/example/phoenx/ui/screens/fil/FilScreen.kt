package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import androidx.navigation.NavController
import com.example.phoenx.ui.components.SecureAsyncImage
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    targetCreatorId: String? = null,
    viewModel: FilViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val selectedRecipientId by viewModel.selectedRecipientId.collectAsState()
    val sortByCreationDate by viewModel.sortByCreationDate.collectAsState()
    val heirKey by viewModel.heirKey.collectAsState()

    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(targetCreatorId) {
        viewModel.setTargetCreator(targetCreatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mon Fil de Pensée", 
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ), 
                                color = theme.contentColor, 
                                fontFamily = theme.fontFamily
                            )
                            Text(text = "${uiState.totalCount} fragments de vie", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
                        }
                        InfoButton(
                            title = "Le Fil de Pensée",
                            points = listOf(
                                "Chaque souvenir est classé par l'âge que tu avais quand tu l'as déposé.",
                                "Pas par date — par âge. C'est la fonctionnalité unique de PHOEN-X.",
                                "Tes proches pourront naviguer et voir comment tu as évolué au fil des années.",
                                "Utilise le slider pour naviguer dans ta propre trajectoire de pensée.",
                                "Chaque entrée porte un Sceau de l'Âge — ton âge exact au moment du dépôt."
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
            } else if (uiState.entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aucun souvenir pour le moment.\nCapture ta première pensée.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.contentColor.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val groupedEntries = uiState.entries.groupBy { it.ageAtCreation.years }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    groupedEntries.keys.sortedDescending().forEach { year ->
                        item { YearSeparator(year, groupedEntries[year]?.size ?: 0) }
                        items(groupedEntries[year] ?: emptyList()) { entry ->
                            if (entry.amendments.isNotEmpty()) {
                                DialogueTemporelItem(
                                    entry = entry,
                                    onClick = { navController.navigate(Screen.MemoryDetail.createRoute(entry.id)) }
                                )
                            } else {
                                TimelineEntryItem(
                                    entry = entry,
                                    heirKey = heirKey,
                                    mediaManager = viewModel.mediaManager,
                                    onClick = { navController.navigate(Screen.MemoryDetail.createRoute(entry.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFilters) {
            ModalBottomSheet(
                onDismissRequest = { showFilters = false },
                sheetState = sheetState,
                containerColor = theme.backgroundColor,
                contentColor = theme.contentColor
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
                    Text("FILTRER LE FIL", style = MaterialTheme.typography.labelSmall, color = accent, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Ordre d'affichage", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !sortByCreationDate,
                            onClick = { if (sortByCreationDate) viewModel.toggleSortOrder() },
                            label = { Text("Par âge") },
                            leadingIcon = { if (!sortByCreationDate) Icon(Icons.Default.Psychology, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                        )
                        FilterChip(
                            selected = sortByCreationDate,
                            onClick = { if (!sortByCreationDate) viewModel.toggleSortOrder() },
                            label = { Text("Par date de création") },
                            leadingIcon = { if (sortByCreationDate) Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Filtrer par destinataire", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedRecipientId == null,
                            onClick = { viewModel.setRecipientFilter(null) },
                            label = { Text("Tous") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                        )
                        recipients.forEach { recipient ->
                            FilterChip(
                                selected = selectedRecipientId == recipient.id,
                                onClick = { viewModel.setRecipientFilter(if (selectedRecipientId == recipient.id) null else recipient.id) },
                                label = { Text(recipient.name) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogueTemporelItem(entry: PhoenXEntry, onClick: () -> Unit) {
    val latestAmendment = entry.amendments.last()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable { onClick() }
            .phoenXMatiere(),
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "DIALOGUE TEMPOREL", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                        color = accent, 
                        letterSpacing = 2.sp
                    )
                }
                Surface(color = theme.backgroundColor, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))) {
                    Text(text = "${latestAmendment.ageAtAmendment.years}a ${latestAmendment.ageAtAmendment.months}m ${latestAmendment.ageAtAmendment.days}j", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = accent, fontSize = 9.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "À ${entry.ageAtCreation.years} ans", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String(entry.encryptedContent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )
                }
                
                Box(modifier = Modifier.padding(horizontal = 12.dp).fillMaxHeight().width(1.dp).background(theme.contentColor.copy(alpha = 0.1f)))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "À ${latestAmendment.ageAtAmendment.years} ans", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                        color = AccentSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String(latestAmendment.encryptedContent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                color = theme.backgroundColor.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, null, tint = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.temporalEvolution ?: "Évolution : Ton apaisement est visible à ${latestAmendment.ageAtAmendment.years - entry.ageAtCreation.years} ans d'intervalle.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun YearSeparator(year: Int, count: Int) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(theme.contentColor.copy(alpha = 0.2f)))
        Text(text = "$year ANS", style = MaterialTheme.typography.labelSmall, color = theme.accentColor, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text(text = "$count pensées", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), fontSize = 10.sp)
    }
}

@Composable
fun TimelineEntryItem(
    entry: PhoenXEntry, 
    heirKey: ByteArray? = null,
    mediaManager: com.example.phoenx.data.media.MediaManager? = null,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH).withZone(ZoneId.systemDefault()) }
    val formattedDate = dateFormatter.format(entry.timestamp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .clickable { onClick() }
            .phoenXMatiere(),
        color = theme.contentColor.copy(alpha = 0.05f),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when(entry.type) {
                        EntryType.THOUGHT -> Icons.Default.Psychology
                        EntryType.PORTRAIT -> Icons.Default.AccountCircle
                        EntryType.QUESTION_ANSWER -> Icons.AutoMirrored.Filled.HelpOutline
                        else -> Icons.Default.HistoryEdu
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(text = entry.type.name, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f), letterSpacing = 1.sp)
                        Text(text = "Créé le $formattedDate", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = theme.contentColor.copy(alpha = 0.3f))
                    }
                    
                    if (entry.isYoungSelfLetter) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = AccentSecondary.copy(alpha = 0.15f), shape = CircleShape) {
                            Text(
                                text = "LETTRE À MES ${entry.targetAge} ANS",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    if (entry.hasEnigma) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Fingerprint, null, tint = accent, modifier = Modifier.size(12.dp))
                    }

                    if (entry.scheduledDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Event, null, tint = Success, modifier = Modifier.size(12.dp))
                    }
                }
                Surface(color = theme.backgroundColor, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))) {
                    Text(text = "${entry.ageAtCreation.years}a ${entry.ageAtCreation.months}m ${entry.ageAtCreation.days}j", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = accent, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (entry.type == EntryType.PHOTO && mediaManager != null) {
                // ... (Existing SecureAsyncImage code)
            }

            val displayText = when(entry.type) {
                EntryType.PORTRAIT -> entry.aiSummary
                EntryType.QUESTION_ANSWER -> entry.aiSummary
                else -> entry.aiSummary.ifBlank { "Souvenir sans titre" }
            }

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = theme.fontFamily
                ),
                color = theme.contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
