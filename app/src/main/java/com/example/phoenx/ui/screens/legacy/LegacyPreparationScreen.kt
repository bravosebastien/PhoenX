package com.example.phoenx.ui.screens.legacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyPreparationScreen(
    onNavigateBack: () -> Unit,
    viewModel: LegacyPreparationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedRecipient by remember { mutableStateOf<RecipientEntity?>(null) }
    val selectedEntries = remember { mutableStateListOf<String>() }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Préparer un Legs", style = MaterialTheme.typography.displaySmall) },
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
            if (selectedRecipient == null) {
                RecipientSelectionList(
                    padding = padding,
                    recipients = uiState.recipients,
                    onSelect = { selectedRecipient = it }
                )
            } else {
                EntrySelectionList(
                    padding = padding,
                    recipient = selectedRecipient!!,
                    entries = uiState.entries,
                    selectedIds = selectedEntries,
                    onToggle = { id ->
                        if (selectedEntries.contains(id)) selectedEntries.remove(id)
                        else selectedEntries.add(id)
                    },
                    onConfirm = {
                        viewModel.saveLegacy(selectedRecipient!!.id, selectedEntries.toList())
                        selectedRecipient = null
                        selectedEntries.clear()
                    },
                    onCancel = { selectedRecipient = null }
                )
            }
        }
    }
}

@Composable
fun RecipientSelectionList(
    padding: PaddingValues,
    recipients: List<RecipientEntity>,
    onSelect: (RecipientEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Choisissez à qui vous voulez transmettre des souvenirs.", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
        items(recipients) { recipient ->
            Surface(
                onClick = { onSelect(recipient) },
                color = SurfaceCard,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(recipient.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Send, null, tint = AccentPrimary)
                }
            }
        }
    }
}

@Composable
fun EntrySelectionList(
    padding: PaddingValues,
    recipient: RecipientEntity,
    entries: List<OfflineEntry>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Pour ${recipient.name}", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Text("Sélectionnez les fragments à transmettre", style = MaterialTheme.typography.headlineSmall)
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                val isSelected = selectedIds.contains(entry.id)
                val age = AgeUtils.parseAgeJson(entry.ageAtCreation)
                
                Surface(
                    onClick = { onToggle(entry.id) },
                    color = if (isSelected) AccentPrimary.copy(alpha = 0.1f) else SurfaceCard.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary) else null
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("À ${age.years} ans", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                            Text(entry.aiSummary.ifEmpty { "Fragment de vie" }, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                        if (isSelected) Icon(Icons.Default.Check, null, tint = AccentPrimary)
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) { Text("Changer de proche", color = TextSecondary) }
            Button(
                onClick = onConfirm,
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Valider ce Legs", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
