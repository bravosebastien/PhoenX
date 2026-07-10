package com.example.phoenx.ui.screens.fil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.screens.capture.RecipientSelector
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    navController: NavController,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsState()
    val content by viewModel.decryptedContent.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val accent = LocalAccentColor.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    android.util.Log.d("MemoryDetailDebug", "MemoryDetailScreen composé, entryId=$entryId")

    var editableText by remember { mutableStateOf("") }
    var isPeriodMode by remember(entry) {
        mutableStateOf(entry?.memoryDateStart != null || entry?.memoryDateEnd != null)
    }

    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    LaunchedEffect(content) {
        if (editableText.isEmpty()) {
            editableText = content
        }
    }

    // Sauvegarde auto du texte avec debounce
    LaunchedEffect(editableText) {
        android.util.Log.d("MemoryDetailDebug", "LaunchedEffect déclenché - editableText='$editableText', content='$content'")
        if (editableText.isNotEmpty() && editableText != content) {
            android.util.Log.d("MemoryDetailDebug", "Condition vraie, attente du debounce...")
            delay(1000)
            android.util.Log.d("MemoryDetailDebug", "Debounce terminé, appel updateContent")
            viewModel.updateContent(editableText)
        } else {
            android.util.Log.d("MemoryDetailDebug", "Condition fausse - pas de sauvegarde déclenchée")
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
            containerColor = BackgroundSecondary,
            title = { Text("Supprimer ce souvenir ?", color = TextPrimary) },
            text = { Text("Cette action est irréversible et supprimera le souvenir de votre fil ainsi que du Cloud.", color = TextSecondary) },
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
                    Text("Annuler", color = TextPrimary)
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
                title = { Text("Édition du souvenir", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // CONTENU DU SOUVENIR (Papier/Parchemin)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)), // Couleur papier
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(32.dp)
                        )
                        TextField(
                            value = editableText,
                            onValueChange = { editableText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF2C2C2E),
                                lineHeight = 28.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                // DATE RÉELLE (MemoryDate / Période)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("QUAND ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Date", style = MaterialTheme.typography.labelSmall, color = if (!isPeriodMode) accent else TextTertiary)
                            Switch(
                                checked = isPeriodMode,
                                onCheckedChange = { isPeriodMode = it },
                                modifier = Modifier.scale(0.7f),
                                colors = SwitchDefaults.colors(checkedThumbColor = accent)
                            )
                            Text("Période", style = MaterialTheme.typography.labelSmall, color = if (isPeriodMode) accent else TextTertiary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (!isPeriodMode) {
                        var showDatePicker by remember { mutableStateOf(false) }
                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entry!!.memoryDate ?: entry!!.createdAt)

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = accent)
                            Spacer(modifier = Modifier.width(12.dp))
                            val dateText = entry!!.memoryDate?.let { 
                                SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(it))
                            } ?: "Ajouter une date précise"
                            Text(dateText, color = TextPrimary)
                        }

                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.updateMemoryDate(datePickerState.selectedDateMillis)
                                        showDatePicker = false
                                    }) { Text("Confirmer", color = accent) }
                                }
                            ) { DatePicker(state = datePickerState) }
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.3f))
                            ) {
                                val txt = entry!!.memoryDateStart?.let { SimpleDateFormat("dd/MM/yy").format(Date(it)) } ?: "Début"
                                Text(txt, color = TextPrimary, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showEndPicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.3f))
                            ) {
                                val txt = entry!!.memoryDateEnd?.let { SimpleDateFormat("dd/MM/yy").format(Date(it)) } ?: "Fin"
                                Text(txt, color = TextPrimary, fontSize = 12.sp)
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
                                }
                            ) { DatePicker(state = startState) }
                        }
                        if (showEndPicker) {
                            DatePickerDialog(
                                onDismissRequest = { showEndPicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.updateMemoryPeriod(entry!!.memoryDateStart, endState.selectedDateMillis)
                                        showEndPicker = false
                                    }) { Text("Confirmer", color = accent) }
                                }
                            ) { DatePicker(state = endState) }
                        }
                    }
                }

                // TIROIRS / COMPARTIMENTS
                Column {
                    Text("DANS QUELS TIROIRS ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
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
                                    selectedLabelColor = BackgroundPrimary
                                )
                            )
                        }
                    }
                }

                // CATÉGORIE ÉMOTIONNELLE
                Column {
                    Text("QUELLE ÉMOTION ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val categories = listOf("Sagesse", "Aventure", "Secret", "Famille", "Amour")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = entry!!.emotionalCategory == cat,
                                onClick = { viewModel.updateCategory(cat) },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = BackgroundPrimary
                                )
                            )
                        }
                    }
                }

                // DESTINATAIRES
                Column {
                    Text("POUR QUI ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val selectedRecipientIds = remember(entry!!.recipientIds) {
                        mutableStateListOf<String>().apply {
                            addAll(entry!!.recipientIds.split(",").filter { it.isNotBlank() })
                        }
                    }

                    RecipientSelector(
                        recipients = recipients,
                        selectedIds = selectedRecipientIds,
                        accent = accent
                    )
                    
                    // On observe les changements du selector pour updater le VM
                    // Correction : observation du contenu réel de la liste
                    LaunchedEffect(selectedRecipientIds.toList()) {
                        val csv = selectedRecipientIds.toList().joinToString(",")
                        if (csv != entry!!.recipientIds) {
                            viewModel.updateRecipients(selectedRecipientIds.toList())
                        }
                    }
                }

                // LIEU (Affichage simple)
                Column {
                    Text("OÙ ?", style = MaterialTheme.typography.labelSmall, color = TextTertiary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = SurfaceCard.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = entry!!.locationName ?: "Lieu non défini",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (entry!!.locationName != null) TextPrimary else TextTertiary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box {
                                IconButton(onClick = { 
                                    if (entry!!.locationName == null) {
                                        navController.navigate(Screen.Map.createRoute(returnToEntryId = entryId))
                                    } else {
                                        showLocationMenu = true
                                    }
                                }) {
                                    Icon(Icons.Default.Edit, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                                }

                                DropdownMenu(
                                    expanded = showLocationMenu,
                                    onDismissRequest = { showLocationMenu = false },
                                    containerColor = BackgroundSecondary
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Changer de lieu", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.EditLocation, null, tint = accent) },
                                        onClick = {
                                            showLocationMenu = false
                                            navController.navigate(Screen.Map.createRoute(returnToEntryId = entryId))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Voir sur la carte", color = TextPrimary) },
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
