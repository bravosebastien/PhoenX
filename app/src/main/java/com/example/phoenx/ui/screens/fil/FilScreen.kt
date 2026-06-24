package com.example.phoenx.ui.screens.fil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.theme.*

@Composable
fun FilScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mon Fil de Pensée",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    IconButton(onClick = { /* Filtres */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = AccentPrimary)
                    }
                }
                Text(
                    text = "${uiState.totalCount} pensées • de ${uiState.minAge} à ${uiState.maxAge} ans",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    ) { padding ->
        val groupedEntries = uiState.entries.groupBy { it.ageAtCreation.years }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
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

@Composable
fun YearSeparator(year: Int, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "• $year ans •",
            style = MaterialTheme.typography.labelLarge,
            color = AccentPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count pensées à cet âge-là",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

@Composable
fun TimelineEntryItem(entry: PhoenXEntry) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color = SurfaceCard,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                    text = "À ${entry.ageAtCreation.years} ans, ${entry.ageAtCreation.months} mois et ${entry.ageAtCreation.days} jours",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = String(entry.encryptedContent), // Démo (normalement déchiffré)
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            SuggestionChip(
                onClick = { },
                label = { Text(entry.type.name, style = MaterialTheme.typography.labelSmall) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = TextSecondary,
                    containerColor = BackgroundPrimary.copy(alpha = 0.5f)
                ),
                border = null
            )
        }
    }
}
