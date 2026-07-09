package com.example.phoenx.ui.screens.mappemonde

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.OfflineEntry
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
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationId) {
        viewModel.loadLocationData(locationId)
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState is LocationDetailUiState.Success) {
                        val loc = (uiState as LocationDetailUiState.Success).location
                        Text("${loc.emoji} ${loc.placeName}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
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
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
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
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, BackgroundPrimary))))
                            
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
                                    color = BackgroundPrimary
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
                                        Icon(Icons.Default.CloudQueue, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text("Aucun souvenir pour le moment.", color = TextSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                items(entries) { entry ->
                                    EditableMemoryCard(
                                        entry = entry,
                                        recipients = recipients,
                                        onUpdate = { viewModel.updateEntrySummary(entry.id, it) },
                                        onUpdateRecipients = { viewModel.updateEntryRecipients(entry.id, it) },
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
                                color = Color(0xFF1E1E23),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E35))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("NOUVEAU SOUVENIR ICI", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.padding(bottom = 12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        MediaAddButton(Icons.Default.CameraAlt, "Caméra", accent, Modifier.weight(1f)) {
                                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_PHOTO, locationId = locationId))
                                        }
                                        MediaAddButton(Icons.Default.PhotoLibrary, "Galerie", accent, Modifier.weight(1f)) {
                                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_GALLERY, locationId = locationId))
                                        }
                                        MediaAddButton(Icons.Default.Mic, "Vocal", accent, Modifier.weight(1f)) {
                                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_AUDIO, locationId = locationId))
                                        }
                                        MediaAddButton(Icons.Default.EditNote, "Texte", accent, Modifier.weight(1f)) {
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
fun MediaAddButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = accent)
        }
    }
}

@Composable
fun EditableMemoryCard(
    entry: OfflineEntry,
    recipients: List<com.example.phoenx.data.local.RecipientEntity>,
    onUpdate: (String) -> Unit,
    onUpdateRecipients: (List<String>) -> Unit,
    onDelete: () -> Unit,
    onDetach: () -> Unit
) {
    val accent = LocalAccentColor.current
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(entry.aiSummary) }
    var showMenu by remember { mutableStateOf(false) }
    var showRecipientDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E35))
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
                    Text(text = "Souvenir du $date", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(text = entry.entryType, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = TextTertiary) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = BackgroundSecondary) {
                        DropdownMenuItem(
                            text = { Text("Modifier", color = TextPrimary) },
                            onClick = { isEditing = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = accent) }
                        )
                        DropdownMenuItem(
                            text = { Text("Changer les destinataires", color = TextPrimary) },
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
                            textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp, color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isEditing = false; editedText = entry.aiSummary }) { Text("Annuler", color = TextSecondary) }
                            TextButton(onClick = { onUpdate(editedText); isEditing = false }) { Text("Sauvegarder", color = accent) }
                        }
                    }
                } else {
                    Text(text = entry.aiSummary, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, lineHeight = 22.sp), color = TextPrimary)
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
            containerColor = BackgroundSecondary,
            title = { Text("Destinataires", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                com.example.phoenx.ui.screens.capture.RecipientSelector(
                    recipients = recipients,
                    selectedIds = selectedIds,
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
                    Text("Valider", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipientDialog = false }) {
                    Text("Annuler", color = TextPrimary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocationDialog(
    location: LocationMemory,
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
        containerColor = BackgroundSecondary,
        title = { Text("Modifier le lieu", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du lieu") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LocalAccentColor.current))
                
                Text("Icône :", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(emojis) { emoji ->
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (selectedEmoji == emoji) LocalAccentColor.current.copy(alpha = 0.25f) else Color.Transparent).border(if (selectedEmoji == emoji) 1.dp else 0.dp, LocalAccentColor.current, CircleShape).clickable { selectedEmoji = emoji }, contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
                
                Text("Période (optionnel) :", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (startDate != null) SimpleDateFormat("dd/MM/yyyy").format(Date(startDate!!)) else "Date début", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (endDate != null) SimpleDateFormat("dd/MM/yyyy").format(Date(endDate!!)) else "Date fin", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, selectedEmoji, startDate, endDate) }, colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current)) { Text("Valider", color = BackgroundPrimary, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextPrimary) } }
    )

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = startDateState.selectedDateMillis
                    showStartPicker = false
                }) { Text("Confirmer", color = LocalAccentColor.current) }
            }
        ) { DatePicker(state = startDateState) }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = endDateState.selectedDateMillis
                    showEndPicker = false
                }) { Text("Confirmer", color = LocalAccentColor.current) }
            }
        ) { DatePicker(state = endDateState) }
    }
}
