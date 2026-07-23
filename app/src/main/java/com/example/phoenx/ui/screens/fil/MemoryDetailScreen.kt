package com.example.phoenx.ui.screens.fil

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.domain.model.CompartmentIds
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val complements by viewModel.complements.collectAsState()
    val textComplements by viewModel.decryptedTextComplements.collectAsState()
    val content by viewModel.decryptedContent.collectAsState()
    val structuredPortrait by viewModel.structuredPortrait.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    // Observation du retour du Picker de lieu
    val pickedLocationId by navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("pickedLocationId", null)?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(pickedLocationId) {
        pickedLocationId?.let { id ->
            viewModel.assignLocationFromId(id)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("pickedLocationId")
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLocationMenu by remember { mutableStateOf(false) }
    var showAddMediaMenu by remember { mutableStateOf(false) }
    var isTiroirsExpanded by remember { mutableStateOf(false) }
    var isTonaliteExpanded by remember { mutableStateOf(false) }

    var editableTitle by remember { mutableStateOf("") }
    var editableText by remember { mutableStateOf("") }
    var isPeriodMode by remember(entry) {
        mutableStateOf(entry?.memoryDateStart != null || entry?.memoryDateEnd != null)
    }

    LaunchedEffect(entryId, targetCreatorId) {
        viewModel.loadEntry(entryId, targetCreatorId)
    }

    LaunchedEffect(entry, content) {
        if (entry != null) {
            if (editableTitle.isEmpty()) editableTitle = entry!!.aiSummary
            if (editableText.isEmpty() || entry!!.parentEntryId != null) {
                editableText = content
            }
        }
    }

    // Sauvegarde auto du titre (Sujet)
    LaunchedEffect(editableTitle) {
        if (entry != null && !entry!!.isChild() && entry!!.entryType != "QUESTION_ANSWER") {
            if (editableTitle.isNotEmpty() && editableTitle != entry!!.aiSummary) {
                delay(1000)
                viewModel.updateTitle(editableTitle)
            }
        }
    }

    // Sauvegarde auto du texte avec debounce
    LaunchedEffect(editableText) {
        if (editableText.isNotEmpty() && editableText != content) {
            delay(1000)
            viewModel.updateContent(editableText)
        }
    }

    // Retour après suppression réussie
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            onNavigateBack()
        }
    }

    // Affichage des erreurs
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Supprimer ce souvenir ?", color = theme.contentColor) },
            text = { Text("Cette action est irréversible et supprimera le souvenir de votre fil ainsi que du Cloud.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.deleteMemory()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Supprimer", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when(entry?.entryType) {
                        "PORTRAIT" -> entry?.aiSummary ?: "Portrait"
                        "QUESTION_ANSWER" -> "Question : ${entry?.aiSummary}"
                        else -> {
                            if (entry?.parentEntryId != null) "Réponse au Portrait"
                            else "L'Étincelle & son Récit"
                        }
                    }
                    Text(titleText, style = MaterialTheme.typography.labelLarge, color = theme.contentColor) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            val isChildEntry = entry!!.parentEntryId != null

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // ÉTAPE 1 : LE SUJET (Titre / Question)
                if (entry!!.entryType != "PORTRAIT") {
                    Column {
                        val subjectLabel = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") "LA QUESTION" else "LE SUJET"
                        Text(subjectLabel, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = theme.contentColor.copy(alpha = 0.05f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") {
                                    Text(
                                        text = entry!!.aiSummary,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = theme.fontFamily,
                                            fontStyle = FontStyle.Italic,
                                            color = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") accent else theme.contentColor
                                        )
                                    )
                                } else {
                                    TextField(
                                        value = editableTitle,
                                        onValueChange = { editableTitle = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { 
                                            Text(
                                                "Donne un titre à ce souvenir...", 
                                                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                                color = theme.contentColor.copy(alpha = 0.4f)
                                            ) 
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = theme.fontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.contentColor
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = theme.contentColor,
                                            unfocusedTextColor = theme.contentColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Titre fixe pour le Portrait
                    Column {
                        Text("LE SUJET", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = entry!!.aiSummary,
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                    }
                }

                // ÉTAPE 2 : LE RÉCIT / LA RÉPONSE
                if (entry!!.entryType == "PORTRAIT") {
                    PortraitAccordion(
                        items = structuredPortrait, 
                        accent = accent,
                        onEditItem = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id, targetCreatorId)) }
                    )
                } else {
                    Column {
                        val récitLabel = if (isChildEntry || entry!!.entryType == "QUESTION_ANSWER") "MA RÉPONSE" else "LE RÉCIT"
                        Text(récitLabel, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (isChildEntry || textComplements.isEmpty()) {
                            // Édition en place pour les réponses atomiques
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                            ) {
                                TextField(
                                    value = editableText,
                                    onValueChange = { editableText = it },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, color = theme.contentColor),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = theme.contentColor,
                                        unfocusedTextColor = theme.contentColor
                                    )
                                )
                            }
                        } else {
                            // Liste des compléments texte
                            textComplements.forEach { (compId, text) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FormatQuote, null, tint = accent.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                                            Row {
                                                IconButton(onClick = { navController.navigate(Screen.MemoryDetail.createRoute(compId, targetCreatorId)) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Edit, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                IconButton(onClick = { viewModel.deleteComplement(compId) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Close, null, tint = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = theme.fontFamily,
                                                lineHeight = 24.sp
                                            ),
                                            color = theme.contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ON CACHE LE RESTE POUR LES RÉPONSES ATOMIQUES (v8.5.9)
                if (!isChildEntry) {
                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp)

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
                            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entry!!.memoryDate ?: entry!!.createdAt)

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
                                        val dateText = entry!!.memoryDate?.let { 
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
                            
                            val startState = rememberDatePickerState(initialSelectedDateMillis = entry!!.memoryDateStart ?: entry!!.createdAt)
                            val endState = rememberDatePickerState(initialSelectedDateMillis = entry!!.memoryDateEnd ?: System.currentTimeMillis())

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
                                    val txt = entry!!.memoryDateStart?.let { SimpleDateFormat("dd/MM/yy").format(Date(it)) } ?: "Début"
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
                                    val txt = entry!!.memoryDateEnd?.let { SimpleDateFormat("dd/MM/yy").format(Date(it)) } ?: "Fin"
                                    Text(txt, color = theme.contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (showStartPicker) {
                                DatePickerDialog(
                                    onDismissRequest = { showStartPicker = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.updateMemoryPeriod(startState.selectedDateMillis, entry!!.memoryDateEnd)
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
                                            viewModel.updateMemoryPeriod(entry!!.memoryDateStart, endState.selectedDateMillis)
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
                                    val currentCompartments = entry!!.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }
                                    val count = currentCompartments.size
                                    val label = if (entry!!.visibility == "EVERYONE") "Tout le monde" else if (count == 0) "Privé" else "$count tiroir(s)"
                                    
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
                                val currentCompartments = entry!!.compartmentIds.trim(',').split(",").filter { it.isNotBlank() }
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

                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp)

                    // COMPLÉMENTS MÉDIA
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("COMPLÉMENTS MÉDIA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                            Box {
                                IconButton(
                                    onClick = { showAddMediaMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.AddCircle, null, tint = accent)
                                }

                                DropdownMenu(
                                    expanded = showAddMediaMenu,
                                    onDismissRequest = { showAddMediaMenu = false },
                                    containerColor = theme.backgroundColor
                                ) {
                                    val types = listOf(
                                        Triple("Texte", Icons.Default.Description, "TEXT"),
                                        Triple("Photo", Icons.Default.PhotoCamera, "PHOTO"),
                                        Triple("Galerie", Icons.Default.Collections, "GALLERY"),
                                        Triple("Vocal", Icons.Default.Mic, "AUDIO")
                                    )
                                    types.forEach { (label, icon, type) ->
                                        DropdownMenuItem(
                                            text = { Text(if (type == "TEXT") "Ajouter un récit" else label, color = theme.contentColor) },
                                            leadingIcon = { Icon(icon, null, tint = accent) },
                                            onClick = {
                                                showAddMediaMenu = false
                                                navController.navigate(Screen.Capture.createRoute(type = type, parentEntryId = entryId))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (complements.isEmpty()) {
                            Text("Aucun média complémentaire rattaché.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.4f))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                complements.filter { it.entryType != "TEXT" }.forEach { complement ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate(
                                                    Screen.MediaViewer.createRoute(complement.id, targetCreatorId)
                                                )
                                            },
                                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (complement.entryType == "PHOTO" || complement.entryType == "GALLERY") {
                                                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                                                    coil3.compose.AsyncImage(
                                                        model = complement.localMediaPath ?: complement.mediaUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    modifier = Modifier.size(60.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = accent.copy(alpha = 0.1f)
                                                ) {
                                                    val icon = when(complement.entryType) {
                                                        "VIDEO" -> Icons.Default.Videocam
                                                        "AUDIO" -> Icons.Default.Mic
                                                        else -> Icons.Default.Description
                                                    }
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = complement.aiSummary.ifEmpty { "Média ${complement.entryType.lowercase()}" },
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = theme.contentColor,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (complement.visibility == "EVERYONE") Icons.Default.Public else Icons.Default.Lock,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(10.dp),
                                                        tint = theme.contentColor.copy(alpha = 0.4f)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = if (complement.visibility == "EVERYONE") "Public" else "Restreint",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = theme.contentColor.copy(alpha = 0.4f)
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { viewModel.deleteComplement(complement.id) }) {
                                                Icon(Icons.Default.DeleteOutline, null, tint = Error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
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
                                    com.example.phoenx.ui.components.InfoPoint(
                                        title = "L'Esprit du Souvenir",
                                        content = "La tonalité influence l'écriture de ton Livre de Vie par l'IA."
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry!!.emotionalCategory,
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
                                            selected = entry!!.emotionalCategory == cat,
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
                                                if (entry!!.emotionalCategory == cat) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp)

                    // DESTINATAIRES
                    Column {
                        Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val selectedRecipientIds = remember(entry!!.recipientIds) {
                            mutableStateListOf<String>().apply {
                                addAll(entry!!.recipientIds.split(",").filter { it.isNotBlank() })
                            }
                        }

                        RecipientSelector(
                            recipients = recipients,
                            selectedIds = selectedRecipientIds,
                            visibility = entry!!.visibility,
                            onVisibilityChange = { viewModel.updateVisibility(it) },
                            accent = accent
                        )
                        
                        LaunchedEffect(selectedRecipientIds.toList()) {
                            val csv = selectedRecipientIds.toList().joinToString(",")
                            if (csv != entry!!.recipientIds) {
                                viewModel.updateRecipients(selectedRecipientIds.toList())
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
                                    if (entry!!.locationName == null) {
                                        navController.navigate(Screen.Map.createRoute(returnToEntryId = entryId))
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
                                    text = entry!!.locationName ?: "Lieu non défini",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (entry!!.locationName != null) theme.contentColor else theme.contentColor.copy(alpha = 0.4f)
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
                                            navController.navigate(Screen.Map.createRoute(returnToEntryId = entryId))
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
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PortraitAccordion(
    items: List<MemoryDetailViewModel.PortraitItem>, 
    accent: Color,
    onEditItem: (String) -> Unit
) {
    var isMasterExpanded by remember { mutableStateOf(false) }
    val theme = LocalAppTheme.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // LE BANDEAU MAÎTRE (v8.5.9)
        Card(
            onClick = { isMasterExpanded = !isMasterExpanded },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoStories, null, tint = accent)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "LES RÉPONSES AU PORTRAIT (${items.size})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isMasterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = accent
                )
            }
        }

        // LE CONTENU DÉROULANT
        AnimatedVisibility(
            visible = isMasterExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEachIndexed { index, item ->
                    var expanded by remember { mutableStateOf(index == 0) }
                    
                    Card(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (expanded) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = accent.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, color = accent)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = item.question.ifBlank { "Pensée libre" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (expanded) accent else theme.contentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (item.id != null) {
                                    IconButton(
                                        onClick = { onEditItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = theme.contentColor.copy(alpha = 0.2f)
                                )
                            }
                            
                            if (expanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.answer,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, lineHeight = 26.sp),
                                    color = theme.contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
