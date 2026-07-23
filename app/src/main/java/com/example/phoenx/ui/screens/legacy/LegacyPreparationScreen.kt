package com.example.phoenx.ui.screens.legacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
    onNavigateToRecipients: () -> Unit,
    viewModel: LegacyPreparationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var selectedRecipient by remember { mutableStateOf<RecipientEntity?>(null) }
    val selectedEntries = remember { mutableStateListOf<String>() }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Préparer un Legs", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            if (selectedRecipient == null) {
                RecipientSelectionList(
                    padding = padding,
                    recipients = uiState.recipients,
                    theme = theme,
                    onSelect = { selectedRecipient = it },
                    onAddRecipient = onNavigateToRecipients
                )
            } else {
                EntrySelectionList(
                    padding = padding,
                    recipient = selectedRecipient!!,
                    entries = uiState.entries,
                    selectedIds = selectedEntries,
                    theme = theme,
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
    theme: AppThemeState,
    onSelect: (RecipientEntity) -> Unit,
    onAddRecipient: () -> Unit
) {
    val accent = theme.accentColor
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Choisissez à qui vous voulez transmettre des souvenirs.", style = MaterialTheme.typography.bodyLarge, color = theme.contentColor.copy(alpha = 0.7f))
        }
        
        if (recipients.isEmpty()) {
            item {
                Button(
                    onClick = onAddRecipient,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.Add, null, tint = accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Ajouter une personne au cercle", color = theme.contentColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(recipients) { recipient ->
            Surface(
                onClick = { onSelect(recipient) },
                color = theme.contentColor.copy(alpha = 0.03f),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(recipient.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Send, null, tint = accent)
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
    theme: AppThemeState,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val accent = theme.accentColor
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Pour ${recipient.name}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Text("Sélectionnez les fragments à transmettre", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
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
                    color = if (isSelected) accent.copy(alpha = 0.1f) else theme.contentColor.copy(alpha = 0.03f),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) accent else theme.contentColor.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("À ${age.years} ans", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                            Text(entry.aiSummary.ifEmpty { "Fragment de vie" }, style = MaterialTheme.typography.bodySmall, color = theme.contentColor)
                        }
                        if (isSelected) Icon(Icons.Default.Check, null, tint = accent)
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) { Text("Changer de proche", color = theme.contentColor.copy(alpha = 0.6f)) }
            Button(
                onClick = onConfirm,
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Valider ce Legs", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
