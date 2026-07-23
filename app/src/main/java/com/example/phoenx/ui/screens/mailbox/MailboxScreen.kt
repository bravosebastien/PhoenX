package com.example.phoenx.ui.screens.mailbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxScreen(
    onNavigateBack: () -> Unit,
    viewModel: MailboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Capsules Temporelles", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
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
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
            } else if (uiState.scheduledItems.isEmpty()) {
                EmptyMailboxContent(modifier = Modifier.padding(padding), theme = theme)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Tes capsules temporelles",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(uiState.scheduledItems) { item ->
                        ScheduledItemCard(
                            item = item,
                            theme = theme,
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduledItemCard(item: OfflineEntry, theme: AppThemeState, onDelete: () -> Unit) {
    val dateText = item.scheduledTimestamp?.let {
        DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(it))
    } ?: "Date inconnue"

    Card(
        modifier = Modifier.fillMaxWidth().phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Success.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Mail, null, tint = Success, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OUVERTURE LE $dateText",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Success
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.aiSummary.ifEmpty { "Message programmé" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = theme.contentColor.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun EmptyMailboxContent(modifier: Modifier = Modifier, theme: AppThemeState) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(64.dp), tint = theme.contentColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Aucune capsule temporelle.", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
        Text("Programme des souvenirs pour des dates futures.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.4f))
    }
}
