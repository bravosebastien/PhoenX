package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.phoenx.domain.model.SimplifiedPerson
import com.example.phoenx.ui.components.EnigmaForm
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.components.LienVivantBanner
import com.example.phoenx.ui.components.LivingLinkDialog
import com.example.phoenx.ui.components.PersonSelector
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
    recipients: List<RecipientEntity>,
    isReadOnly: Boolean = false
) {
    val selectedPersons by viewModel.selectedPersons.collectAsState()
    val suggestedPersons by viewModel.suggestedPersons.collectAsState()
    val selectedRecipientIds by viewModel.selectedRecipientIds.collectAsState()
    val hasSeenIncludeInBookNudge by viewModel.hasSeenIncludeInBookNudge.collectAsState()
    val allPacts by viewModel.allPacts.collectAsState()

    var isPeriodMode by remember(entry) {
        mutableStateOf(entry.memoryDateStart != null || entry.memoryDateEnd != null)
    }
    var isTiroirsExpanded by remember { mutableStateOf(false) }
    var isTonaliteExpanded by remember { mutableStateOf(false) }
    var showLocationMenu by remember { mutableStateOf(false) }
    var showIncludeInBookNudge by remember { mutableStateOf(false) }
    var showMirrorSelection by remember { mutableStateOf(false) }
    var showLivingLinkDialog by remember { mutableStateOf(false) } // v9.4.27

    // ÉNIGME / COFFRE-FORT (v9.4.27)
    var enigmaEnabled by remember(entry) { mutableStateOf(entry.enigmaQuestion != null) }
    var enigmaQuestion by remember(entry) { mutableStateOf(entry.enigmaQuestion ?: "") }
    var enigmaAnswer by remember { mutableStateOf("") }
    var enigmaHint by remember(entry) { mutableStateOf(entry.enigmaHint ?: "") }
    var autoUnlockDays by remember(entry) { mutableStateOf(entry.enigmaAutoUnlockDays ?: 30) }
    var isUltimateSecret by remember(entry) { mutableStateOf(entry.isUltimateSecret) }

    LaunchedEffect(enigmaEnabled, enigmaQuestion, enigmaAnswer, enigmaHint, autoUnlockDays, isUltimateSecret) {
        if (!isReadOnly) {
            val wasEnabled = entry.enigmaQuestion != null
            val hasChanged = enigmaEnabled != wasEnabled || 
                (enigmaEnabled && (
                    enigmaQuestion != (entry.enigmaQuestion ?: "") ||
                    enigmaAnswer.isNotEmpty() ||
                    enigmaHint != (entry.enigmaHint ?: "") ||
                    autoUnlockDays != (entry.enigmaAutoUnlockDays ?: 30) ||
                    isUltimateSecret != entry.isUltimateSecret
                ))

            if (hasChanged) {
                kotlinx.coroutines.delay(1000)
                if (enigmaEnabled) {
                    viewModel.updateEnigma(enigmaQuestion, enigmaAnswer.ifBlank { null }, enigmaHint, autoUnlockDays, isUltimateSecret)
                } else {
                    viewModel.updateEnigma(null, null, null, null, false)
                }
            }
        }
    }

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

    Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
        // ── SECTION 2 : QUAND ET OÙ ────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("QUAND ET OÙ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f), letterSpacing = 2.sp)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.contentColor.copy(alpha = 0.04f), // Renforcé
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.15f)) // Renforcé
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // DATE RÉELLE
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LE MOMENT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Date", style = MaterialTheme.typography.labelSmall, color = if (!isPeriodMode) accent else theme.contentColor.copy(alpha = 0.4f))
                                Switch(
                                    checked = isPeriodMode,
                                    onCheckedChange = { isPeriodMode = it },
                                    modifier = Modifier.scale(0.7f),
                                    colors = SwitchDefaults.colors(checkedThumbColor = accent),
                                    enabled = !isReadOnly
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
                                    .then(if (!isReadOnly) Modifier.clickable { showDatePicker = true } else Modifier)
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
                                    onClick = { if (!isReadOnly) showStartPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                                    enabled = !isReadOnly
                                ) {
                                    Icon(Icons.Default.CalendarToday, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    val txt = entry.memoryDateStart?.let { SimpleDateFormat("dd/MM/yy").format(Date(it)) } ?: "Début"
                                    Text(txt, color = theme.contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { if (!isReadOnly) showEndPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                                    enabled = !isReadOnly
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

                    // LIEU
                    Column {
                        Text("LE LIEU", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (!isReadOnly) {
                                        if (entry.locationName == null) {
                                            navController.navigate(Screen.Map.createRoute(returnToEntryId = entry.id))
                                        } else {
                                            showLocationMenu = true
                                        }
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
                                if (!isReadOnly) {
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
            }
        }

        // ── SECTION 3 : POUR QUI ──────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("POUR QUI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f), letterSpacing = 2.sp)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.contentColor.copy(alpha = 0.04f), // Renforcé
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.15f)) // Renforcé
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // DESTINATAIRES
                    Column {
                        Text("DESTINATAIRES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        RecipientSelector(
                            recipients = recipients,
                            selectedIds = selectedRecipientIds,
                            onToggleRecipient = { if (!isReadOnly) viewModel.toggleRecipient(it) },
                            visibility = entry.visibility,
                            onVisibilityChange = { if (!isReadOnly) viewModel.updateVisibility(it) },
                            accent = accent,
                            notifyByEmail = !entry.silentAttribution,
                            onNotifyByEmailChange = { if (!isReadOnly) viewModel.updateSilentAttribution(!it) },
                            enabled = !isReadOnly
                        )

                        // v9.4.27 : Bloc LIEN VIVANT (Transmission Active)
                        if (!isReadOnly) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLivingLinkDialog = true }
                                    .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                color = theme.contentColor.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color = accent.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.IosShare, null, tint = accent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Transmettre un Lien Vivant", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                                        Text("Envoyer ce souvenir maintenant ou plus tard.", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
                                }
                            }
                        }

                        // v9.4.27 : Bloc Souveraineté Premium (Livre de Vie)
                        if (!isReadOnly) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        val newValue = !entry.includeInBook
                                        if (newValue && !hasSeenIncludeInBookNudge) {
                                            showIncludeInBookNudge = true
                                        }
                                        viewModel.updateIncludeInBook(newValue) 
                                    }
                                    .border(1.dp, if (entry.includeInBook) accent.copy(alpha = 0.3f) else theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                color = if (entry.includeInBook) accent.copy(alpha = 0.05f) else theme.contentColor.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color = if (entry.includeInBook) accent.copy(alpha = 0.1f) else theme.contentColor.copy(alpha = 0.05f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.AutoStories, null, tint = if (entry.includeInBook) accent else theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Inclure dans mon Livre", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                                            Spacer(Modifier.width(8.dp))
                                            InfoPoint(
                                                title = "Aide au Livre de Vie",
                                                content = "En incluant ce souvenir, il sera repris et reformulé par l'IA pour nourrir votre Livre de Vie. L'IA n'utilise que vos résumés pour rédiger un récit fluide avec ses propres mots : votre texte original reste strictement privé.\n\nAttention : Le chapitre créé sera visible par tous les destinataires de votre Livre, même si ce souvenir était initialement réservé à certains d'entre eux seulement."
                                            )
                                        }
                                        Text("Nourrir le récit IA par ce souvenir.", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                                    }
                                    Switch(
                                        checked = entry.includeInBook,
                                        onCheckedChange = { 
                                            if (it && !hasSeenIncludeInBookNudge) {
                                                showIncludeInBookNudge = true
                                            }
                                            viewModel.updateIncludeInBook(it) 
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
                                    )
                                }
                            }
                        }

                        // Lien Vivant
                        if (selectedRecipientIds.size == 1) {
                            val recipientId = selectedRecipientIds.first()
                            val recipient = recipients.find { it.id == recipientId }
                            if (recipient != null) {
                                Spacer(modifier = Modifier.height(24.dp))
                                LienVivantBanner(recipientName = recipient.name, recipientPhone = recipient.phone)
                            }
                        }
                    }

                    // TIROIRS
                    Column {
                        Text("DANS QUELS TIROIRS ?", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = theme.contentColor.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTiroirsExpanded = !isTiroirsExpanded }
                                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            color = theme.contentColor.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                val currentCompartments = entry.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }.map { it.trim() }
                                Text(text = if (entry.visibility == "EVERYONE") "Tout le monde" else if (currentCompartments.isEmpty()) "Privé" else "${currentCompartments.size} tiroir(s)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                                Icon(imageVector = if (isTiroirsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = theme.contentColor.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                            }
                        }
                        AnimatedVisibility(visible = isTiroirsExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                val currentCompartments = entry.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CompartmentIds.ALL.forEach { id ->
                                        val isSelected = currentCompartments.contains(id)
                                        FilterChip(
                                            selected = isSelected, 
                                            onClick = { 
                                                if (!isReadOnly) { 
                                                    val isRemoving = isSelected
                                                    val newList = if (isRemoving) currentCompartments - id else currentCompartments + id
                                                    
                                                    if (id == CompartmentIds.LE_PACTE) {
                                                        if (isRemoving) {
                                                            viewModel.updatePactId(null)
                                                            viewModel.updateCompartments(newList)
                                                        } else {
                                                            when (allPacts.size) {
                                                                0 -> { /* On laisse l'utilisateur créer un miroir d'abord */ }
                                                                1 -> {
                                                                    viewModel.updatePactId(allPacts.first().id)
                                                                    viewModel.updateCompartments(newList)
                                                                }
                                                                else -> {
                                                                    showMirrorSelection = true
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        viewModel.updateCompartments(newList) 
                                                    }
                                                } 
                                            }, 
                                            label = { Text(CompartmentIds.getLabel(id)) }, 
                                            enabled = !isReadOnly || isSelected, 
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor, containerColor = theme.contentColor.copy(alpha = 0.05f), labelColor = theme.contentColor.copy(alpha = 0.6f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // PERSONNAGES
                    if (!isReadOnly || selectedPersons.isNotEmpty()) {
                        PersonSelector(selectedPersons = selectedPersons, suggestedPersons = suggestedPersons, onSearch = { viewModel.searchPersons(it) }, onSelect = { viewModel.selectPerson(it) }, onSelectMe = { viewModel.selectMe() }, onCreate = { f, l, r, dt, dv, uri, ct -> viewModel.createAndSelectPerson(f, l, r, dt, dv, uri, ct) }, onRemove = { viewModel.removePerson(it) }, onManageCharacters = { navController.navigate(Screen.Characters.route) }, accent = accent, enabled = !isReadOnly)
                    }
                }
            }
        }

        // ── SECTION 4 : PROTECTION ──────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PROTECTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.6f), letterSpacing = 2.sp)
            EnigmaForm(
                isEnabled = enigmaEnabled,
                onToggleEnabled = { enigmaEnabled = it },
                question = enigmaQuestion,
                onQuestionChange = { enigmaQuestion = it },
                answer = enigmaAnswer,
                onAnswerChange = { enigmaAnswer = it },
                hasExistingAnswer = entry.enigmaAnswer != null,
                hint = enigmaHint,
                onHintChange = { enigmaHint = it },
                autoUnlockDays = autoUnlockDays,
                onAutoUnlockDaysChange = { autoUnlockDays = it ?: 30 },
                isUltimateSecret = isUltimateSecret,
                onUltimateSecretToggle = { isUltimateSecret = it },
                theme = theme,
                accent = accent,
                isReadOnly = isReadOnly
            )
        }
    }

    if (showIncludeInBookNudge) {
        AlertDialog(
            onDismissRequest = { showIncludeInBookNudge = false },
            containerColor = theme.backgroundColor,
            title = { Text("Livre de Vie", color = theme.contentColor, style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "En incluant ce souvenir, il sera repris et reformulé par l'IA pour nourrir votre Livre de Vie. L'IA n'utilise que vos résumés pour rédiger un récit fluide avec ses propres mots : votre texte original reste strictement privé.\n\nAttention : Le chapitre créé sera visible par tous les destinataires de votre Livre, même si ce souvenir était initialement réservé à certains d'entre eux seulement.",
                    color = theme.contentColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.markIncludeInBookNudgeSeen(); showIncludeInBookNudge = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Ne plus afficher", color = theme.backgroundColor) }
            },
            dismissButton = {
                TextButton(onClick = { showIncludeInBookNudge = false }) {
                    Text("Fermer", color = theme.contentColor)
                }
            }
        )
    }

    if (showMirrorSelection) {
        // ... (existant)
    }

    if (showLivingLinkDialog) {
        LivingLinkDialog(
            recipients = recipients,
            onDismiss = { showLivingLinkDialog = false },
            onConfirm = { rid, date -> 
                viewModel.sendLivingLink(rid, date)
                showLivingLinkDialog = false 
            }
        )
    }
}
