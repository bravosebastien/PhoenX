package com.example.phoenx.ui.screens.mappemonde

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    locationId: String,
    navController: NavController,
    mode: MapMode = MapMode.CREATOR,
    viewModel: LocationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    
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

    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationId) {
        viewModel.loadLocationData(locationId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState is LocationDetailUiState.Success) {
                        val loc = (uiState as LocationDetailUiState.Success).location
                        Text("${loc.emoji} ${loc.placeName}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic), color = theme.contentColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (mode == MapMode.CREATOR && uiState is LocationDetailUiState.Success) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, "Modifier le lieu", tint = accent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            when (uiState) {
                is LocationDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
                }
                is LocationDetailUiState.Success -> {
                    val data = uiState as LocationDetailUiState.Success
                    val location = data.location
                    val entries = data.entries
                    val recipients by viewModel.recipients.collectAsState()

                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        // EN-TÊTE : Période et Mini-Carte
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 13f)
                                },
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = false,
                                    scrollGesturesEnabled = false,
                                    zoomGesturesEnabled = false,
                                    tiltGesturesEnabled = false,
                                    rotationGesturesEnabled = false
                                )
                            ) {
                                Marker(state = MarkerState(position = LatLng(location.latitude, location.longitude)))
                            }
                            // Overlay dégradé
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, theme.backgroundColor))))
                            
                            // Badge Période
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                color = accent.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val start = if (location.startDate != null) SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(Date(location.startDate)) else "???"
                                val end = if (location.endDate != null) SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(Date(location.endDate)) else "En cours"
                                Text(
                                    text = "$start — $end",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = theme.backgroundColor
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "SOUVENIRS ÉPINGLÉS (${entries.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                            }

                            if (entries.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.CloudQueue, null, tint = theme.contentColor.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text("Aucun souvenir pour le moment.", color = theme.contentColor.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                items(entries) { entry ->
                                    EditableMemoryCard(
                                        entry = entry,
                                        recipients = recipients,
                                        theme = theme,
                                        onUpdate = { viewModel.updateEntrySummary(entry.id, it) },
                                        onUpdateRecipients = { viewModel.updateEntryRecipients(entry.id, it) },
                                        onUpdateVisibility = { viewModel.updateEntryVisibility(entry.id, it) },
                                        onDelete = { viewModel.deleteEntry(entry.id) },
                                        onDetach = { viewModel.detachEntry(entry.id) }
                                    )
                                }
                            }
                        }

                        // ACTIONS DE CRÉATION DIRECTE
                        if (mode == MapMode.CREATOR) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = theme.contentColor.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("NOUVEAU SOUVENIR ICI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        MediaAddButton(Icons.Default.CameraAlt, "Caméra", accent, theme, Modifier.weight(1f)) {
                                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_PHOTO, locationId = locationId))
                                        }
                                        MediaAddButton(Icons.Default.PhotoLibrary, "Galerie", accent, theme, Modifier.weight(1f)) {
                                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_GALLERY, locationId = locationId))
                                        }
                                        MediaAddButton(Icons.Default.Mic, "Vocal", accent, theme, Modifier.weight(1f)) {
                                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_AUDIO, locationId = locationId))
                                        }
                                        MediaAddButton(Icons.Default.EditNote, "Texte", accent, theme, Modifier.weight(1f)) {
                                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_TEXT, locationId = locationId))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showEditDialog) {
                        EditLocationDialog(
                            location = location,
                            theme = theme,
                            datePickerColors = datePickerColors,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { name, emoji, start, end ->
                                viewModel.updateLocation(location.id, name, emoji, start, end)
                                showEditDialog = false
                            }
                        )
                    }
                }
                is LocationDetailUiState.Error -> {
                    Text("Erreur lors du chargement.", modifier = Modifier.align(Alignment.Center), color = Error)
                }
            }
        }
    }
}

@Composable
fun MediaAddButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, theme: AppThemeState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

@Composable
fun EditableMemoryCard(
    entry: OfflineEntry,
    recipients: List<com.example.phoenx.data.local.RecipientEntity>,
    theme: AppThemeState,
    onUpdate: (String) -> Unit,
    onUpdateRecipients: (List<String>) -> Unit,
    onUpdateVisibility: (String) -> Unit,
    onDelete: () -> Unit,
    onDetach: () -> Unit
) {
    val accent = theme.accentColor
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(entry.aiSummary) }
    var showMenu by remember { mutableStateOf(false) }
    var showRecipientDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = accent.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when(entry.entryType) {
                            "PHOTO" -> Icons.Default.CameraAlt
                            "VIDEO" -> Icons.Default.Videocam
                            "AUDIO" -> Icons.Default.Mic
                            else -> Icons.Default.Description
                        }
                        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val date = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(entry.createdAt))
                    Text(text = "Souvenir du $date", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                    Text(text = entry.entryType, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = theme.contentColor.copy(alpha = 0.4f)) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = theme.backgroundColor) {
                        DropdownMenuItem(
                            text = { Text("Modifier", color = theme.contentColor) },
                            onClick = { isEditing = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = accent) }
                        )
                        DropdownMenuItem(
                            text = { Text("Changer les destinataires", color = theme.contentColor) },
                            onClick = { showRecipientDialog = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.People, null, tint = accent) }
                        )
                        DropdownMenuItem(
                            text = { Text("Détacher", color = Warning) },
                            onClick = { onDetach(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.LinkOff, null, tint = Warning) }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = Error) },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Error) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                if (isEditing) {
                    Column {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 15.sp, color = theme.contentColor),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isEditing = false; editedText = entry.aiSummary }) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
                            TextButton(onClick = { onUpdate(editedText); isEditing = false }) { Text("Sauvegarder", color = accent) }
                        }
                    }
                } else {
                    Text(text = entry.aiSummary, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, lineHeight = 22.sp), color = theme.contentColor)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showRecipientDialog) {
        val selectedIds = remember { 
            mutableStateListOf<String>().apply { 
                addAll(entry.recipientIds.split(",").filter { it.isNotBlank() }) 
            }
        }
        AlertDialog(
            onDismissRequest = { showRecipientDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Destinataires", fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, color = theme.contentColor) },
            text = {
                RecipientSelector(
                    recipients = recipients,
                    selectedIds = selectedIds,
                    visibility = entry.visibility,
                    onVisibilityChange = { onUpdateVisibility(it) },
                    accent = accent
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        onUpdateRecipients(selectedIds.toList())
                        showRecipientDialog = false 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Valider", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipientDialog = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocationDialog(
    location: LocationMemory,
    theme: AppThemeState,
    datePickerColors: DatePickerColors,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?, Long?) -> Unit
) {
    var name by remember { mutableStateOf(location.placeName) }
    var selectedEmoji by remember { mutableStateOf(location.emoji) }
    var startDate by remember { mutableStateOf(location.startDate) }
    var endDate by remember { mutableStateOf(location.endDate) }
    
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    val startDateState = rememberDatePickerState(initialSelectedDateMillis = startDate ?: System.currentTimeMillis())
    val endDateState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: System.currentTimeMillis())

    val emojis = listOf("📍", "🏠", "🏖️", "🏔️", "🌆", "🌿", "🏛️", "🎭", "🍷", "🎿", "🌊", "🏕️")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Modifier le lieu", fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, color = theme.contentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Nom du lieu") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accentColor, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                Text("Icône :", style = MaterialTheme.typography.labelMedium, color = theme.contentColor.copy(alpha = 0.6f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(emojis) { emoji ->
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (selectedEmoji == emoji) theme.accentColor.copy(alpha = 0.25f) else Color.Transparent).border(if (selectedEmoji == emoji) 1.dp else 0.dp, theme.accentColor, CircleShape).clickable { selectedEmoji = emoji }, contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
                
                Text("Période (optionnel) :", style = MaterialTheme.typography.labelMedium, color = theme.contentColor.copy(alpha = 0.6f))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor)
                    ) {
                        Text(if (startDate != null) SimpleDateFormat("dd/MM/yyyy").format(Date(startDate!!)) else "Date début", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor)
                    ) {
                        Text(if (endDate != null) SimpleDateFormat("dd/MM/yyyy").format(Date(endDate!!)) else "Date fin", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, selectedEmoji, startDate, endDate) }, colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)) { Text("Valider", color = theme.backgroundColor, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor) } }
    )

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = startDateState.selectedDateMillis
                    showStartPicker = false
                }) { Text("Confirmer", color = theme.accentColor) }
            },
            colors = datePickerColors
        ) { DatePicker(state = startDateState, colors = datePickerColors) }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = endDateState.selectedDateMillis
                    showEndPicker = false
                }) { Text("Confirmer", color = theme.accentColor) }
            },
            colors = datePickerColors
        ) { DatePicker(state = endDateState, colors = datePickerColors) }
    }
}
