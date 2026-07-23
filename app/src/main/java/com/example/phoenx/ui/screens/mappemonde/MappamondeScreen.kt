package com.example.phoenx.ui.screens.mappemonde

import android.content.Context
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.R
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappamondeScreen(
    navController: NavController,
    mode: MapMode = MapMode.CREATOR,
    returnToEntryId: String? = null,
    viewModel: MappamondeViewModel = hiltViewModel()
) {
    val visibleLocations by viewModel.visibleLocations.collectAsState()
    val allLocations by viewModel.allLocations.collectAsState()
    val trailPoints by viewModel.trailPoints.collectAsState()
    val lastAppeared by viewModel.lastAppearedLocation.collectAsState()
    val isGlobeView by viewModel.isGlobeView.collectAsState()
    val timelineAge by viewModel.timelineAge.collectAsState()
    val currentAge by viewModel.currentAge.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val accent = LocalAccentColor.current
    val theme = LocalAppTheme.current
    val backgroundBrush = LocalBackgroundBrush.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
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

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showInventory by remember { mutableStateOf(false) }
    var selectedLocationWithEntries by remember { mutableStateOf<LocationWithEntries?>(null) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var pendingLatLng by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder()
            .target(LatLng(20.0, 0.0))
            .zoom(if (isGlobeView) 2.0f else 3.0f)
            .build()
    }

    val mapStyleOptions = remember {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_phoenx)
    }

    LaunchedEffect(Unit) {
        viewModel.setMode(mode)
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        // ── La Carte Google Maps ──────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                // RESTAURATION : Vue claire (NORMAL standard Google Maps)
                mapType = if (isGlobeView) MapType.HYBRID else MapType.NORMAL,
                isMyLocationEnabled = false,
                mapStyleOptions = null // On retire le style "noir" pour retrouver le blanc/gris clair
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                rotationGesturesEnabled = true,
                zoomGesturesEnabled = true
            ),
            onMapLongClick = { latLng ->
                if (mode == MapMode.CREATOR || mode == MapMode.PICKER) {
                    pendingLatLng = latLng
                    showAddLocationDialog = true
                }
            }
        ) {
            // Fil d'or chronologique
            if (trailPoints.size >= 2 && mode != MapMode.PICKER) {
                Polyline(
                    points = trailPoints,
                    color = accent,
                    width = 6f,
                    geodesic = true
                )
            }

            // Moteur de Clustering Intelligent
            Clustering(
                items = visibleLocations,
                onClusterClick = { cluster ->
                    // ACTION : Zoomer sur le groupe au clic
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(cluster.position, cameraPositionState.position.zoom + 2f),
                            800
                        )
                    }
                    true
                },
                onClusterItemClick = { item ->
                    if (mode == MapMode.PICKER && returnToEntryId != null) {
                        // Retour avec l'ID du lieu sélectionné
                        navController.previousBackStackEntry?.savedStateHandle?.set("pickedLocationId", item.location.id)
                        navController.popBackStack()
                    } else {
                        // Navigation directe vers le QG du lieu
                        navController.navigate("location_detail/${item.location.id}")
                    }
                    true
                },
                clusterContent = { cluster ->
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(accent, CircleShape)
                            .shadow(8.dp, CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cluster.size.toString(),
                            color = Color(0xFF1A1A1F),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                    }
                },
                clusterItemContent = { item ->
                    MarqueurSouvenir(memoriesCount = item.location.memoriesCount)
                }
            )
        }

        // ── Overlay Haut : Titre + Recherche ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(theme.backgroundColor.copy(alpha = 0.95f), Color.Transparent)))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accent)
                }
                Text(
                    text = "Ma Mappemonde",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontSize = 22.sp),
                    color = theme.contentColor,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showInventory = true }) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = accent)
                }
                InfoButton(
                    title = "La Mappemonde",
                    points = listOf(
                        "Maintiens ton doigt sur un point de la carte pour créer un marqueur.",
                        "Tape sur un marqueur pour voir et modifier ses souvenirs.",
                        "Le fil doré relie tes lieux dans l'ordre chronologique.",
                        "Le slider en bas permet de filtrer tes lieux par âge.",
                        "Recherche une adresse avec la barre flottante."
                    )
                )
            }

            // Barre de recherche flottante
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        textStyle = TextStyle(color = theme.contentColor, fontSize = 15.sp, fontFamily = theme.fontFamily),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) Text("Chercher un lieu ou une adresse...", color = theme.contentColor.copy(alpha = 0.4f), fontSize = 15.sp)
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            coroutineScope.launch {
                                searchAddress(context, searchQuery, cameraPositionState) { isSearching = it }
                            }
                        }),
                        singleLine = true
                    )
                    if (isSearching) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ── Boutons flottants à droite ──
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.toggleMapView() },
                containerColor = SurfaceCard,
                contentColor = accent,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                // RESTAURATION : Icône d'origine pour la bascule
                Icon(imageVector = if (isGlobeView) Icons.Default.Map else Icons.Default.Public, contentDescription = null)
            }
        }

        // ── Instructions quand vide ──
        if (!isLoading && allLocations.isEmpty() && mode == MapMode.CREATOR) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = theme.backgroundColor.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, null, tint = accent, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Ta Mappemonde est vierge", style = MaterialTheme.typography.titleMedium, color = theme.contentColor)
                    Spacer(Modifier.height(8.dp))
                    Text("Maintiens ton doigt sur le globe pour épingler ton premier souvenir géographique.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                }
            }
        }

        // ── Slider Timeline Flottant ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            GeographicTimeline(
                currentAge = currentAge,
                selectedAge = timelineAge,
                onAgeChange = { viewModel.onTimelineSlide(it) }
            )
        }

        // ── BottomSheets & Dialogs ──
        if (selectedLocationWithEntries != null) {
            LocationBottomSheet(
                data = selectedLocationWithEntries!!,
                mode = mode,
                viewModel = viewModel,
                navController = navController,
                onClose = { selectedLocationWithEntries = null }
            )
        }

        if (showInventory) {
            InventoryBottomSheet(
                locations = allLocations,
                onClose = { showInventory = false },
                onSelect = { 
                    showInventory = false
                    navController.navigate("location_detail/${it.location.id}")
                }
            )
        }

        if (showAddLocationDialog && pendingLatLng != null) {
            AddLocationDialog(
                latLng = pendingLatLng!!,
                onConfirm = { placeName, emoji, visitedAt ->
                    coroutineScope.launch {
                        val newId = viewModel.pinLocation(pendingLatLng!!, placeName, "", emoji, visitedAt)
                        if (mode == MapMode.PICKER && returnToEntryId != null && newId != null) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("pickedLocationId", newId)
                            navController.popBackStack()
                        }
                        showAddLocationDialog = false
                    }
                },
                onDismiss = { showAddLocationDialog = false }
            )
        }
    }
}

@Composable
fun MarqueurSouvenir(memoriesCount: Int) {
    val accent = LocalAccentColor.current
    val size = when {
        memoriesCount == 0 -> 24.dp
        memoriesCount <= 3 -> 36.dp
        memoriesCount <= 10 -> 48.dp
        else -> 60.dp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(animation = tween(2200, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(size * scale).background(accent.copy(alpha = 0.25f), CircleShape))
        Surface(
            modifier = Modifier.size(size).shadow(8.dp, CircleShape),
            shape = CircleShape,
            color = accent,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.6f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (memoriesCount >= 11) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFF1A1A1F), modifier = Modifier.size((size.value * 0.5f).dp))
                } else if (memoriesCount > 0) {
                    Text(memoriesCount.toString(), color = Color(0xFF1A1A1F), fontWeight = FontWeight.Bold, fontSize = (size.value * 0.35f).sp)
                } else {
                    Box(modifier = Modifier.size(5.dp).background(Color(0xFF1A1A1F), CircleShape))
                }
            }
        }
    }
}

@Composable
fun GeographicTimeline(currentAge: Int, selectedAge: Int, onAgeChange: (Int) -> Unit) {
    val accent = LocalAccentColor.current
    val theme = LocalAppTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TRAJECTOIRE DE VIE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                Text(if (selectedAge >= currentAge) "Aujourd'hui" else "$selectedAge ans", color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = selectedAge.toFloat(),
                onValueChange = { onAgeChange(it.toInt()) },
                valueRange = 0f..currentAge.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = theme.contentColor.copy(alpha = 0.1f))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBottomSheet(
    data: LocationWithEntries,
    mode: MapMode,
    viewModel: MappamondeViewModel,
    navController: NavController,
    onClose: () -> Unit
) {
    val location = data.location
    val entries = data.entries
    val accent = LocalAccentColor.current
    val theme = LocalAppTheme.current

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
    
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(location.placeName) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = if (location.visitedAt > 0) location.visitedAt else System.currentTimeMillis()
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = theme.backgroundColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.contentColor.copy(alpha = 0.2f)) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 44.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(location.emoji, fontSize = 36.sp)
                Spacer(modifier = Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontFamily = theme.fontFamily, fontSize = 20.sp, color = theme.contentColor),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.updateLocationName(location.id, editedName); isEditingName = false }) {
                                    Icon(Icons.Default.Check, null, tint = accent)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(location.placeName, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                            IconButton(onClick = { isEditingName = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, null, tint = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    // Date éditable
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        val dateStr = if (location.visitedAt > 0) {
                            SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(Date(location.visitedAt))
                        } else "Ajouter une date"
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = if (location.visitedAt > 0) theme.contentColor.copy(alpha = 0.6f) else accent)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "${entries.size} souvenir(s) à cet endroit", style = MaterialTheme.typography.labelMedium, color = accent)
            
            Spacer(modifier = Modifier.height(14.dp))

            // Carrousel des souvenirs
            if (entries.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(entries) { entry ->
                        Card(
                            modifier = Modifier.width(280.dp).height(140.dp),
                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoStories, null, tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("SOUVENIR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = entry.aiSummary.ifEmpty { "Souvenir précieux..." },
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, lineHeight = 18.sp),
                                    color = theme.contentColor.copy(alpha = 0.8f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(90.dp).background(theme.contentColor.copy(alpha = 0.03f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Text("Aucun souvenir attaché ici.", color = theme.contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            if (mode == MapMode.CREATOR) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = { 
                            navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_TEXT, locationId = location.id))
                            onClose() 
                        },
                        modifier = Modifier.weight(1.4f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = BackgroundPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Épingler", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { navController.navigate("location_detail/${location.id}"); onClose() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Détails", color = theme.contentColor)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = { viewModel.removeLocation(location.id); onClose() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Supprimer ce lieu", color = Error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dateState.selectedDateMillis?.let { viewModel.updateLocationDate(location.id, it) }
                        showDatePicker = false
                    }) { Text("Confirmer", color = accent) }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler", color = theme.contentColor) } },
                colors = datePickerColors
            ) {
                DatePicker(state = dateState, colors = datePickerColors)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryBottomSheet(locations: List<LocationWithEntries>, onClose: () -> Unit, onSelect: (LocationWithEntries) -> Unit) {
    val theme = LocalAppTheme.current
    ModalBottomSheet(onDismissRequest = onClose, containerColor = theme.backgroundColor) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Mes Lieux Épinglés", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
            Spacer(modifier = Modifier.height(20.dp))
            if (locations.isEmpty()) {
                Text("Aucun lieu pour le moment.", color = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(locations) { item ->
                        Card(
                            onClick = { onSelect(item) },
                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.location.emoji, fontSize = 28.sp)
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.location.placeName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                                    Text("${item.entries.size} souvenirs", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddLocationDialog(latLng: LatLng, onConfirm: (String, String, Long) -> Unit, onDismiss: () -> Unit) {
    var placeName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📍") }
    val emojis = listOf("📍", "🏠", "🏖️", "🏔️", "🌆", "🌿", "🏛️", "🎭", "🍷", "🎿", "🌊", "🏕️")
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    val theme = LocalAppTheme.current

    LaunchedEffect(latLng) {
        withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            results?.firstOrNull()?.let { placeName = it.locality ?: it.countryName ?: "" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Nouveau lieu", fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold, color = theme.contentColor) },
        text = {
            Column {
                OutlinedTextField(
                    value = placeName, 
                    onValueChange = { placeName = it }, 
                    label = { Text("Nom du lieu") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                Spacer(Modifier.height(20.dp))
                Text("Icône :", style = MaterialTheme.typography.labelMedium, color = theme.contentColor.copy(alpha = 0.6f))
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(emojis) { emoji ->
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (selectedEmoji == emoji) accent.copy(alpha = 0.25f) else Color.Transparent).border(if (selectedEmoji == emoji) 1.dp else 0.dp, accent, CircleShape).clickable { selectedEmoji = emoji }, contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(placeName, selectedEmoji, System.currentTimeMillis()) }, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(10.dp)) { Text("Épingler", color = theme.backgroundColor, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor) } }
    )
}

suspend fun searchAddress(context: Context, query: String, cameraPositionState: CameraPositionState, onSearchStateChange: (Boolean) -> Unit) {
    if (query.isBlank()) return
    onSearchStateChange(true)
    try {
        withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(query, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                withContext(Dispatchers.Main) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 14f), 1200)
                }
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Lieu introuvable", Toast.LENGTH_SHORT).show() }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("Mappemonde", "Erreur", e)
    } finally {
        onSearchStateChange(false)
    }
}
