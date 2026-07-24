package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.domain.model.CompartmentIds
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.components.LienVivantBanner
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.AppThemeState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryMetadataSection(
    entry: OfflineEntry,
    viewModel: MemoryDetailViewModel,
    theme: AppThemeState,
    accent: Color,
    navController: NavController,
    recipients: List<RecipientEntity>
) {
    var isPeriodMode by remember(entry) {
        mutableStateOf(entry.memoryDateStart != null || entry.memoryDateEnd != null)
    }
    var isTiroirsExpanded by remember { mutableStateOf(false) }
    var isTonaliteExpanded by remember { mutableStateOf(false) }
    var showLocationMenu by remember { mutableStateOf(false) }

    val datePickerColors = DatePickerDefaults.colors(
        containerColor = theme.backgroundColor,
        titleContentColor = theme.contentColor,
        headlineContentColor = theme.contentColor,
        weekdayContentColor = theme.contentColor.copy(alpha = 0.4f),
        subheadContentColor = theme.contentColor.copy(alpha = 0.4f),
        yearContentColor = theme.contentColor,
        currentYearContentColor = accent,
        selectedYearContentColor = theme.backgroundColor,
        selectedYearContainerColor = accent,
        dayContentColor = theme.contentColor,
        disabledDayContentColor = theme.contentColor.copy(alpha = 0.1f),
        selectedDayContentColor = theme.backgroundColor,
        selectedDayContainerColor = accent,
        todayContentColor = accent,
        todayDateBorderColor = accent
    )

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        // DATE RÉELLE (MemoryDate / Période)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("QUAND ?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = if (!isPeriodMode) accent else theme.contentColor.copy(alpha = 0.4f))
                    Switch(
                        checked = isPeriodMode,
                        onCheckedChange = { isPeriodMode = it },
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
                    )
                    Text("Période", style = MaterialTheme.typography.labelSmall, color = if (isPeriodMode) accent else theme.contentColor.copy(alpha = 0.4f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!isPeriodMode) {
                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entry.memoryDate ?: entry.createdAt)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    color = theme.contentColor.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DATE PRÉCISE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            val dateText = entry.memoryDate?.let { 
                                SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(it))
                            } ?: "Ajouter une date"
                            Text(dateText, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.updateMemoryDate(datePickerState.selectedDateMillis)
                                showDatePicker = false
                            }) { Text("Confirmer", color = accent) }
                        },
                        colors = datePickerColors
                    ) { DatePicker(state = datePickerState, colors = datePickerColors) }
                }
            } else {
                // MODE PÉRIODE
                var showStartPicker by remember { mutableStateOf(false) }
                var showEndPicker by remember { mutableStateOf(false) }
                
                val startState = rememberDatePickerState(initialSelectedDateMillis = entry.memoryDateStart ?: entry.createdAt)
                val endState = rememberDatePickerState(initialSelectedDateMillis = entry.memoryDateEnd ?: System.currentTimeMillis())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.contentColor.copy(alpha = 0.03f))
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        val txt = entry.memoryDateStart?.let { SimpleDateFormat("dd/MM/yy").format(Date(it)) } ?: "Début"
                        Text(txt, color = theme.contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.contentColor.copy(alpha = 0.03f))
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        val txt = entry.memoryDateEnd?.let { SimpleDateFormat("dd/MM/yy").format(Date(it)) } ?: "Fin"
                        Text(txt, color = theme.contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showStartPicker) {
                    DatePickerDialog(
                        onDismissRequest = { showStartPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.updateMemoryPeriod(startState.selectedDateMillis, entry.memoryDateEnd)
                                showStartPicker = false
                            }) { Text("Confirmer", color = accent) }
                        },
                        colors = datePickerColors
                    ) { DatePicker(state = startState, colors = datePickerColors) }
                }
                if (showEndPicker) {
                    DatePickerDialog(
                        onDismissRequest = { showEndPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.updateMemoryPeriod(entry.memoryDateStart, endState.selectedDateMillis)
                                showEndPicker = false
                            }) { Text("Confirmer", color = accent) }
                        },
                        colors = datePickerColors
                    ) { DatePicker(state = endState, colors = datePickerColors) }
                }
            }
        }

        // TIROIRS / COMPARTIMENTS (v8.9.2 : Menu déroulant)
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTiroirsExpanded = !isTiroirsExpanded }
                    .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                color = theme.contentColor.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "DANS QUELS TIROIRS ?", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val currentCompartments = entry.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }
                        val count = currentCompartments.size
                        val label = if (entry.visibility == "EVERYONE") "Tout le monde" else if (count == 0) "Privé" else "$count tiroir(s)"
                        
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isTiroirsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = theme.contentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = isTiroirsExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    val currentCompartments = entry.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompartmentIds.ALL.forEach { id ->
                            val isSelected = currentCompartments.contains(id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newList = if (isSelected) currentCompartments - id else currentCompartments + id
                                    viewModel.updateCompartments(newList)
                                },
                                label = { Text(CompartmentIds.getLabel(id)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = theme.backgroundColor,
                                    containerColor = theme.contentColor.copy(alpha = 0.05f),
                                    labelColor = theme.contentColor.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // CATÉGORIE ÉMOTIONNELLE (v8.9.2 : Menu déroulant)
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTonaliteExpanded = !isTonaliteExpanded }
                    .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                color = theme.contentColor.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "QUELLE TONALITÉ ?", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        InfoPoint(
                            title = "L'Esprit du Souvenir",
                            content = "La tonalité influence l'écriture de ton Livre de Vie par l'IA."
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.emotionalCategory,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isTonaliteExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = theme.contentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = isTonaliteExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    val categories = listOf("Sagesse", "Aventure", "Secret", "Famille", "Amour", "Nostalgie", "Humour", "Leçon", "Voyage", "Quotidien", "Épreuve")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = entry.emotionalCategory == cat,
                                onClick = { viewModel.updateCategory(cat) },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = theme.backgroundColor,
                                    containerColor = theme.contentColor.copy(alpha = 0.05f),
                                    labelColor = theme.contentColor.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(
                                    1.dp, 
                                    if (entry.emotionalCategory == cat) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // DESTINATAIRES
        Column {
            Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            val selectedRecipientIds = remember(entry.recipientIds) {
                mutableStateListOf<String>().apply {
                    addAll(entry.recipientIds.split(",").filter { it.isNotBlank() })
                }
            }

            RecipientSelector(
                recipients = recipients,
                selectedIds = selectedRecipientIds,
                visibility = entry.visibility,
                onVisibilityChange = { viewModel.updateVisibility(it) },
                accent = accent,
                notifyByEmail = !entry.silentAttribution,
                onNotifyByEmailChange = { viewModel.updateSilentAttribution(!it) }
            )
            
            LaunchedEffect(selectedRecipientIds.toList()) {
                val csv = selectedRecipientIds.toList().joinToString(",")
                if (csv != entry.recipientIds) {
                    viewModel.updateRecipients(selectedRecipientIds.toList())
                }
            }

            // NOUVEAUTÉ v8.9.8 : Lien Vivant
            if (selectedRecipientIds.size == 1) {
                val recipientId = selectedRecipientIds.first()
                val recipient = recipients.find { it.id == recipientId }
                if (recipient != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    LienVivantBanner(
                        recipientName = recipient.name,
                        recipientPhone = recipient.phone
                    )
                }
            }
        }

        // LIEU
        Column {
            Text("OÙ ?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (entry.locationName == null) {
                            navController.navigate(Screen.Map.createRoute(returnToEntryId = entry.id))
                        } else {
                            showLocationMenu = true
                        }
                    }
                    .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                color = theme.contentColor.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = entry.locationName ?: "Lieu non défini",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (entry.locationName != null) theme.contentColor else theme.contentColor.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, null, tint = theme.contentColor.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))

                    DropdownMenu(
                        expanded = showLocationMenu,
                        onDismissRequest = { showLocationMenu = false },
                        containerColor = theme.backgroundColor
                    ) {
                        DropdownMenuItem(
                            text = { Text("Changer de lieu", color = theme.contentColor) },
                            leadingIcon = { Icon(Icons.Default.EditLocation, null, tint = accent) },
                            onClick = {
                                showLocationMenu = false
                                navController.navigate(Screen.Map.createRoute(returnToEntryId = entry.id))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Voir sur la carte", color = theme.contentColor) },
                            leadingIcon = { Icon(Icons.Default.Map, null, tint = accent) },
                            onClick = {
                                showLocationMenu = false
                                navController.navigate(Screen.Map.createRoute())
                            }
                        )
                    }
                }
            }
        }
    }
}
