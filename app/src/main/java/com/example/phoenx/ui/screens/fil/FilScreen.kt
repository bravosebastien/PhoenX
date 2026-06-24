package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mon Fil de Pensée",
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.totalCount} fragments de vie",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Filtres */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = AccentPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPrimary)
            }
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
            
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(BackgroundSecondary, BackgroundPrimary),
                    radius = 2000f
                )
            )) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    groupedEntries.keys.sortedDescending().forEach { year ->
                        item {
                            YearSeparator(year, groupedEntries[year]?.size ?: 0)
                        }
                        items(groupedEntries[year] ?: emptyList()) { entry ->
                            TimelineEntryItem(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearSeparator(year: Int, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(TextTertiary.copy(alpha = 0.3f)))
        Text(
            text = "$year ANS",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Text(
            text = "$count pensées",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TimelineEntryItem(entry: PhoenXEntry) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        color = SurfaceCard.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = if (entry.type == EntryType.THOUGHT) Icons.Default.Psychology else Icons.Default.HistoryEdu
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
                
                // Le Sceau de l'Âge
                Surface(
                    color = BackgroundPrimary,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${entry.ageAtCreation.months}m ${entry.ageAtCreation.days}j",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = String(entry.encryptedContent), // Contenu déchiffré
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 26.sp
            )
        }
    }
}
