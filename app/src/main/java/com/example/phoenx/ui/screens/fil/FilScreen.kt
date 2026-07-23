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
    var filterFavoritesOnly by remember { mutableStateOf(false) }
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
                // Séparation des souvenirs attribués et non attribués (v8.9.2)
                val allEntries = if (filterFavoritesOnly) {
                    uiState.entries.filter { it.tags.contains("FAVORITE") }
                } else {
                    uiState.entries
                }

                val notAttributed = allEntries.filter { it.recipientIds.isEmpty() }
                val attributed = allEntries.filter { it.recipientIds.isNotEmpty() }
                val groupedAttributed = attributed.groupBy { it.ageAtCreation.years }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // SECTION 1 : NON ATTRIBUÉS (Priorité)
                    if (notAttributed.isNotEmpty()) {
                        item {
                            YearSeparator(
                                year = 0, // Code pour "Non attribué"
                                count = notAttributed.size,
                                labelOverride = "À ATTRIBUER"
                            )
                        }
                        items(notAttributed) { entry ->
                            TimelineEntryItem(
                                entry = entry,
                                heirKey = heirKey,
                                mediaManager = viewModel.mediaManager,
                                isNonAttributed = true,
                                onClick = { navController.navigate(Screen.MemoryDetail.createRoute(entry.id)) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }

                    // SECTION 2 : PAR ÂGE
                    groupedAttributed.keys.sortedDescending().forEach { year ->
                        item { YearSeparator(year, groupedAttributed[year]?.size ?: 0) }
                        items(groupedAttributed[year] ?: emptyList()) { entry ->
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

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Mes coups de cœur", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FilterChip(
                        selected = filterFavoritesOnly,
                        onClick = { filterFavoritesOnly = !filterFavoritesOnly },
                        label = { Text("Voir mes coups de cœur uniquement") },
                        leadingIcon = { if (filterFavoritesOnly) Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                    )
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
fun YearSeparator(year: Int, count: Int, labelOverride: String? = null) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = labelOverride ?: "$year ANS", 
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 2.sp,
                    fontFamily = theme.fontFamily
                ), 
                color = if (labelOverride != null) Error else accent
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f).height(1.dp).background(if (labelOverride != null) Error.copy(alpha = 0.2f) else accent.copy(alpha = 0.15f)))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$count ${if (count > 1) "pensées" else "pensée"}", 
                style = MaterialTheme.typography.labelSmall, 
                color = theme.contentColor.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun TimelineEntryItem(
    entry: PhoenXEntry, 
    heirKey: ByteArray? = null,
    mediaManager: com.example.phoenx.data.media.MediaManager? = null,
    isNonAttributed: Boolean = false,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH).withZone(ZoneId.systemDefault()) }
    val formattedDate = dateFormatter.format(entry.timestamp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // Petit indicateur de type (point coloré)
                val dotColor = when(entry.type) {
                    EntryType.PHOTO -> Color(0xFF4CAF50)
                    EntryType.AUDIO -> Color(0xFF2196F3)
                    EntryType.VIDEO -> Color(0xFFFFC107)
                    EntryType.PORTRAIT -> Color(0xFFE91E63)
                    EntryType.QUESTION_ANSWER -> Color(0xFF9C27B0)
                    else -> accent
                }
                Box(modifier = Modifier.size(6.dp).background(if (isNonAttributed) Error else dotColor, CircleShape))
                
                Spacer(modifier = Modifier.width(12.dp))

                val displayText = when(entry.type) {
                    EntryType.PORTRAIT -> entry.aiSummary
                    EntryType.QUESTION_ANSWER -> entry.aiSummary
                    else -> entry.aiSummary.ifBlank { "Souvenir sans titre" }
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = theme.fontFamily,
                        fontWeight = if (entry.amendments.isNotEmpty()) FontWeight.Black else FontWeight.Medium
                    ),
                    color = if (isNonAttributed) theme.contentColor.copy(alpha = 0.8f) else theme.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (entry.amendments.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.AutoAwesome, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                }
                
                if (entry.hasEnigma) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Fingerprint, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Date / Âge compact
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isNonAttributed) "À attribuer" else "${entry.ageAtCreation.years}a ${entry.ageAtCreation.months}m",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isNonAttributed) Error else accent.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.3f)
                )
            }
        }
        
        // Ligne de séparation fine
        Spacer(modifier = Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(theme.contentColor.copy(alpha = 0.05f)))
    }
}
