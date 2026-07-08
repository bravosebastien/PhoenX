package com.example.phoenx.ui.screens.mappemonde

import android.location.Geocoder
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappamondeScreen(
    navController: NavController,
    mode: MapMode = MapMode.CREATOR,
    viewModel: MappamondeViewModel = hiltViewModel()
) {
    val visibleLocations by viewModel.visibleLocations.collectAsState()
    val trailPoints by viewModel.trailPoints.collectAsState()
    val lastAppeared by viewModel.lastAppearedLocation.collectAsState()
    val isGlobeView by viewModel.isGlobeView.collectAsState()
    val timelineAge by viewModel.timelineAge.collectAsState()
    val currentAge by viewModel.currentAge.collectAsState()
    val canShowTimeline by viewModel.canShowTimeline.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    var selectedLocationWithEntries by remember { mutableStateOf<LocationWithEntries?>(null) }

    var showAddLocationDialog by remember { mutableStateOf(false) }
    var pendingLatLng by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder()
            .target(LatLng(20.0, 0.0))
            .zoom(if (isGlobeView) 2.0f else 5.0f)
            .build()
    }

    val mapStyleOptions = remember {
        MapStyleOptions.loadRawResourceStyle(context, com.example.phoenx.R.raw.map_style_phoenx)
    }

    LaunchedEffect(Unit) {
        viewModel.setMode(mode)
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        bottomBar = {
            // Afficher la timeline seulement si au moins 2 souvenirs à des âges différents
            if (canShowTimeline) {
                GeographicTimeline(
                    currentAge = currentAge,
                    selectedAge = timelineAge,
                    onAgeChange = { viewModel.onTimelineSlide(it) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Le Globe ou Carte Google Maps ──────────────────
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = if (isGlobeView) MapType.HYBRID else MapType.NORMAL,
                    isMyLocationEnabled = false,
                    mapStyleOptions = mapStyleOptions
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    rotationGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = true,
                    zoomGesturesEnabled = true
                ),
                onMapLongClick = { latLng ->
                    if (mode == MapMode.CREATOR) {
                        pendingLatLng = latLng
                        showAddLocationDialog = true
                    }
                }
            ) {
                if (trailPoints.size >= 2) {
                    Polyline(
                        points = trailPoints,
                        color = AccentPrimary,
                        width = 4f,
                        geodesic = true
                    )
                }

                if (lastAppeared != null) {
                    MarkerInfoWindow(
                        state = rememberMarkerState(position = LatLng(lastAppeared!!.latitude, lastAppeared!!.longitude)),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        content = {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E35)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = lastAppeared!!.placeName,
                                    color = Color(0xFFF2EDE8),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    )
                }

                Clustering(
                    items = visibleLocations,
                    onClusterClick = { true },
                    onClusterItemClick = { item ->
                        selectedLocationWithEntries = item
                        true
                    },
                    clusterContent = { cluster ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(AccentPrimary, CircleShape)
                                .shadow(4.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cluster.size.toString(),
                                color = Color(0xFF1A1A1F),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    clusterItemContent = { item ->
                        MarqueurSouvenir(memoriesCount = item.location.memoriesCount)
                    }
                )
            }

            // ── Titre et Overlay Haut ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BackgroundPrimary.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                    Text(
                    text = "Ma Mappemonde",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                InfoButton(
                    title = "La Mappemonde",
                    points = listOf(
                        "Épingle des souvenirs sur n'importe quel lieu dans le monde.",
                        "Maintiens ton doigt sur un point de la carte pour créer un marqueur.",
                        "Plus un lieu contient de souvenirs, plus son marqueur est grand et brillant.",
                        "Utilise le slider en bas pour voir ta trajectoire géographique de vie.",
                        "Le fil doré relie tes lieux dans l'ordre chronologique."
                    )
                )
                Text(
                    text = "${visibleLocations.size} lieux",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary
                    )
                }
            }

            // ── Barre de recherche flottante ──────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 110.dp) // Sous le titre, aligné avec le bouton globe
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF01E1E23)
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = LocalAccentColor.current,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        textStyle = TextStyle(
                            color = Color(0xFFF2EDE8),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Chercher un lieu ou une adresse...",
                                    color = Color(0xFF5C5855),
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                coroutineScope.launch {
                                    searchAddress(
                                        context = context,
                                        query = searchQuery,
                                        cameraPositionState = cameraPositionState,
                                        onSearchStateChange = { isSearching = it }
                                    )
                                }
                            }
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFF5C5855),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = LocalAccentColor.current,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // ── Toggle Globe / Classic ─────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 180.dp, end = 16.dp) // Décalé vers le bas car la barre de recherche prend de la place
            ) {
                FloatingActionButton(
                    onClick = { viewModel.toggleMapView() },
                    containerColor = SurfaceCard,
                    contentColor = AccentPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isGlobeView) Icons.Default.Map else Icons.Default.Public,
                        contentDescription = "Changer de vue"
                    )
                }
            }

            // ── Instructions ──
            if (visibleLocations.isEmpty() && mode == MapMode.CREATOR) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .background(BackgroundPrimary.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Maintiens ton doigt sur un lieu\npour y épingler un souvenir",
                        style = TextStyle(fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    )
                }
            }

            // ── BottomSheet : fiche du lieu sélectionné
            if (selectedLocationWithEntries != null) {
                LocationBottomSheet(
                    data = selectedLocationWithEntries!!,
                    mode = mode,
                    onClose = { selectedLocationWithEntries = null },
                    onViewMemories = {
                        navController.navigate("location_detail/${selectedLocationWithEntries!!.location.id}")
                        selectedLocationWithEntries = null
                    },
                    onAddMemory = {
                        navController.navigate("capture?locationId=${selectedLocationWithEntries!!.location.id}")
                        selectedLocationWithEntries = null
                    },
                    onDelete = {
                        viewModel.removeLocation(selectedLocationWithEntries!!.location.id)
                        selectedLocationWithEntries = null
                    }
                )
            }

            // ── Dialog ajout nouveau lieu ─────────────
            if (showAddLocationDialog && pendingLatLng != null) {
                AddLocationDialog(
                    latLng = pendingLatLng!!,
                    onConfirm = { placeName, emoji, visitedAt ->
                        viewModel.pinLocation(pendingLatLng!!, placeName, "", emoji, visitedAt)
                        showAddLocationDialog = false
                    },
                    onDismiss = { showAddLocationDialog = false }
                )
            }
        }
    }
}

@Composable
fun MarqueurSouvenir(memoriesCount: Int) {
    val size = when {
        memoriesCount == 0 -> 24.dp
        memoriesCount <= 3 -> 32.dp
        memoriesCount <= 10 -> 40.dp
        else -> 52.dp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // Halo pulsant
        Box(
            modifier = Modifier
                .size(size * scale)
                .background(AccentPrimary.copy(alpha = 0.3f), CircleShape)
        )
        // Cercle intérieur fixe
        Box(
            modifier = Modifier
                .size(size)
                .background(AccentPrimary, CircleShape)
                .shadow(4.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (memoriesCount >= 11) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF1A1A1F),
                    modifier = Modifier.size(24.dp)
                )
            } else if (memoriesCount > 0) {
                Text(
                    text = memoriesCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1F),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun GeographicTimeline(
    currentAge: Int,
    selectedAge: Int,
    onAgeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1F))
            .padding(top = 8.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            .border(width = 1.dp, color = Color(0xFF2E2E35), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Trajectoire de vie",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF5C5855),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            letterSpacing = 1.sp
        )

        Slider(
            value = selectedAge.toFloat(),
            onValueChange = { onAgeChange(it.toInt()) },
            valueRange = 0f..currentAge.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = AccentPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = Color(0xFF2E2E35)
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Naissance", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5C5855))
            Text("$selectedAge ans", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9B9590), fontWeight = FontWeight.Bold)
            Text("Aujourd'hui", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5C5855))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBottomSheet(
    data: LocationWithEntries,
    mode: MapMode,
    onClose: () -> Unit,
    onViewMemories: () -> Unit,
    onAddMemory: () -> Unit,
    onDelete: () -> Unit
) {
    val location = data.location
    val entries = data.entries

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = BackgroundSecondary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(location.emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(location.placeName, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif), color = TextPrimary)
                    Text(location.countryName, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${entries.size} souvenir(s) à cet endroit",
                style = MaterialTheme.typography.labelMedium,
                color = AccentPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Mini-carrousel des résumés
            if (entries.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(entries.take(5)) { entry: com.example.phoenx.data.local.OfflineEntry ->
                        Card(
                            modifier = Modifier
                                .width(240.dp)
                                .height(120.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.AutoStories, null, tint = AccentPrimary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = entry.aiSummary.ifEmpty { "Souvenir capturé..." },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        lineHeight = 18.sp
                                    ),
                                    color = TextSecondary,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(SurfaceCard, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun résumé disponible", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (mode == MapMode.CREATOR) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onAddMemory,
                        modifier = Modifier.weight(1f).height(56.dp).phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = BackgroundPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Épingler", color = BackgroundPrimary)
                    }
                    
                    OutlinedButton(
                        onClick = onViewMemories,
                        modifier = Modifier.weight(1f).height(56.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary)
                    ) {
                        Text("Tout voir", color = TextPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Supprimer ce lieu", color = Color(0xFFE57373), style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Button(
                    onClick = onViewMemories,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Découvrir les souvenirs", color = BackgroundPrimary)
                }
            }
        }
    }
}

@Composable
fun AddLocationDialog(
    latLng: LatLng,
    onConfirm: (String, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var placeName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📍") }
    val emojis = listOf("📍", "🏖️", "🏔️", "🌆", "🏛️", "🌿", "🏝️", "🎭", "🍷", "🎿", "🌊", "🏕️")

    LaunchedEffect(latLng) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            placeName = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.countryName ?: "Lieu inconnu"
        } catch (e: Exception) {
            placeName = "Lieu inconnu"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Épingler ce souvenir", color = TextPrimary, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)) },
        text = {
            Column {
                OutlinedTextField(
                    value = placeName,
                    onValueChange = { placeName = it },
                    label = { Text("Nom du lieu") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPrimary)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("Choisir un symbole :", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.height(100.dp)) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selectedEmoji == emoji) AccentPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedEmoji = emoji }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(placeName, selectedEmoji, System.currentTimeMillis()) }, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                Text("Épingler", color = BackgroundPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}

/**
 * Fonction de recherche d'adresse par géocodage
 */
suspend fun searchAddress(
    context: Context,
    query: String,
    cameraPositionState: CameraPositionState,
    onSearchStateChange: (Boolean) -> Unit
) {
    if (query.isBlank()) return
    onSearchStateChange(true)
    try {
        withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(query, 1)
            
            if (!results.isNullOrEmpty()) {
                val location = results[0]
                val latLng = LatLng(location.latitude, location.longitude)
                withContext(Dispatchers.Main) {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(latLng, 14f),
                        durationMs = 800
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lieu introuvable. Essaie une autre adresse.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("Mappemonde", "Erreur géocodage", e)
    } finally {
        onSearchStateChange(false)
    }
}
