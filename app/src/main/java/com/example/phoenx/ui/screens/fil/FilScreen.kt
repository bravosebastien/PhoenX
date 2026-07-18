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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(targetCreatorId) {
        viewModel.setTargetCreator(targetCreatorId)
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Mon Fil de Pensée", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                            Text(text = "${uiState.totalCount} fragments de vie", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = AccentPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentPrimary)
            } else if (uiState.entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aucun souvenir pour le moment.\nCapture ta première pensée.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextTertiary,
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
                containerColor = BackgroundSecondary,
                contentColor = TextPrimary
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
                    Text("FILTRER LE FIL", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Ordre d'affichage", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !sortByCreationDate,
                            onClick = { if (sortByCreationDate) viewModel.toggleSortOrder() },
                            label = { Text("Par âge") },
                            leadingIcon = { if (!sortByCreationDate) Icon(Icons.Default.Psychology, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentPrimary, selectedLabelColor = BackgroundPrimary)
                        )
                        FilterChip(
                            selected = sortByCreationDate,
                            onClick = { if (!sortByCreationDate) viewModel.toggleSortOrder() },
                            label = { Text("Par date de création") },
                            leadingIcon = { if (sortByCreationDate) Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentPrimary, selectedLabelColor = BackgroundPrimary)
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
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentPrimary, selectedLabelColor = BackgroundPrimary)
                        )
                        recipients.forEach { recipient ->
                            FilterChip(
                                selected = selectedRecipientId == recipient.id,
                                onClick = { viewModel.setRecipientFilter(if (selectedRecipientId == recipient.id) null else recipient.id) },
                                label = { Text(recipient.name) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentPrimary, selectedLabelColor = BackgroundPrimary)
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable { onClick() }
            .phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DIALOGUE TEMPOREL", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 2.sp)
                }
                Surface(color = BackgroundPrimary, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))) {
                    Text(text = "${latestAmendment.ageAtAmendment.years}a ${latestAmendment.ageAtAmendment.months}m ${latestAmendment.ageAtAmendment.days}j", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = AccentPrimary, fontSize = 9.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("À ${entry.ageAtCreation.years} ans", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String(entry.encryptedContent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                }
                
                Box(modifier = Modifier.padding(horizontal = 12.dp).fillMaxHeight().width(1.dp).background(TextTertiary.copy(alpha = 0.2f)))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("À ${latestAmendment.ageAtAmendment.years} ans", style = MaterialTheme.typography.labelSmall, color = AccentSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String(latestAmendment.encryptedContent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                color = BackgroundPrimary.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.temporalEvolution ?: "Évolution : Ton apaisement est visible à ${latestAmendment.ageAtAmendment.years - entry.ageAtCreation.years} ans d'intervalle.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun YearSeparator(year: Int, count: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(TextTertiary.copy(alpha = 0.3f)))
        Text(text = "$year ANS", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text(text = "$count pensées", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
    }
}

@Composable
fun TimelineEntryItem(
    entry: PhoenXEntry, 
    heirKey: ByteArray? = null,
    mediaManager: com.example.phoenx.data.media.MediaManager? = null,
    onClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH).withZone(ZoneId.systemDefault()) }
    val formattedDate = dateFormatter.format(entry.timestamp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .clickable { onClick() }
            .phoenXMatiere(),
        color = SurfaceCard.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = if (entry.type == EntryType.THOUGHT) Icons.Default.Psychology else Icons.Default.HistoryEdu
                    Icon(imageVector = icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(text = entry.type.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 1.sp)
                        Text(text = "Créé le $formattedDate", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = TextTertiary)
                    }
                    
                    if (entry.isYoungSelfLetter) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = AccentSecondary.copy(alpha = 0.2f), shape = CircleShape) {
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
                        Icon(Icons.Default.Fingerprint, null, tint = AccentPrimary, modifier = Modifier.size(12.dp))
                    }

                    if (entry.scheduledDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Event, null, tint = Success, modifier = Modifier.size(12.dp))
                    }
                }
                Surface(color = BackgroundPrimary, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))) {
                    Text(text = "${entry.ageAtCreation.years}a ${entry.ageAtCreation.months}m ${entry.ageAtCreation.days}j", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = AccentPrimary, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (entry.type == EntryType.PHOTO && mediaManager != null) {
                SecureAsyncImage(
                    mediaUrl = entry.mediaUrl,
                    localPath = entry.localMediaPath,
                    explicitKey = heirKey,
                    mediaManager = mediaManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .padding(bottom = 12.dp)
                )
            }

            Text(text = String(entry.encryptedContent), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, lineHeight = 26.sp)
        }
    }
}
