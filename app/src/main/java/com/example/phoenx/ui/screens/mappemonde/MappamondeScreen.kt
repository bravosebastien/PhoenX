package com.example.phoenx.ui.screens.mappemonde

import android.location.Geocoder
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappamondeScreen(
    navController: NavController,
    mode: MapMode = MapMode.CREATOR,
    viewModel: MappamondeViewModel = hiltViewModel()
) {
    val locations by viewModel.locations.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val isGlobeView by viewModel.isGlobeView.collectAsState()
    val context = LocalContext.current

    var showAddLocationDialog by remember { mutableStateOf(false) }
    var pendingLatLng by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(Unit) {
        viewModel.setMode(mode)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Le Globe ou Carte Google Maps ──────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.builder()
                    .target(LatLng(20.0, 0.0))
                    .zoom(if (isGlobeView) 2.0f else 5.0f)
                    .build()
            },
            properties = MapProperties(
                mapType = if (isGlobeView) MapType.HYBRID else MapType.NORMAL,
                isMyLocationEnabled = false
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
            locations.forEach { location ->
                LocationMarker(
                    location = location,
                    onClick = { viewModel.selectLocation(location) }
                )
            }
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
                Text(
                    text = "${locations.size} lieux",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary
                )
            }
        }

        // ── Toggle Globe / Classic ─────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 110.dp, end = 16.dp)
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
        if (locations.isEmpty() && mode == MapMode.CREATOR) {
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
        if (selectedLocation != null) {
            LocationBottomSheet(
                location = selectedLocation!!,
                mode = mode,
                onClose = { viewModel.selectLocation(null) },
                onViewMemories = {
                    navController.navigate("location_detail/${selectedLocation!!.id}")
                    viewModel.selectLocation(null)
                },
                onAddMemory = {
                    navController.navigate("capture?locationId=${selectedLocation!!.id}")
                    viewModel.selectLocation(null)
                },
                onDelete = {
                    viewModel.removeLocation(selectedLocation!!.id)
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

@Composable
fun LocationMarker(location: LocationMemory, onClick: () -> Unit) {
    val radius = when {
        location.memoriesCount >= 4 -> 14.dp
        location.memoriesCount >= 1 -> 10.dp
        else -> 8.dp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "scale"
    )

    MarkerComposable(
        state = MarkerState(position = LatLng(location.latitude, location.longitude)),
        onClick = { onClick(); true }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Halo
            Box(
                modifier = Modifier
                    .size(radius * 2.5f * scale)
                    .background(AccentPrimary.copy(alpha = 0.2f), CircleShape)
            )
            // Cercle principal
            Box(
                modifier = Modifier
                    .size(radius * 2f)
                    .shadow(4.dp, CircleShape)
                    .background(AccentPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(location.emoji, fontSize = (radius.value * 0.8f).sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBottomSheet(
    location: LocationMemory,
    mode: MapMode,
    onClose: () -> Unit,
    onViewMemories: () -> Unit,
    onAddMemory: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = SurfaceCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(location.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(location.placeName, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text("${location.latitude}, ${location.longitude}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Visité le ${java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(location.visitedAt))}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            val countText = if (location.memoriesCount > 0) "${location.memoriesCount} souvenir(s) attaché(s)" else "Aucun souvenir attaché"
            Text(
                text = countText,
                style = MaterialTheme.typography.labelLarge,
                color = if (location.memoriesCount > 0) AccentPrimary else TextTertiary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF3E3E45))
            Spacer(modifier = Modifier.height(24.dp))

            if (mode == MapMode.CREATOR) {
                Button(
                    onClick = onAddMemory,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un souvenir", color = BackgroundPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onViewMemories,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary)
                ) {
                    Text("Voir les souvenirs", color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Supprimer ce lieu", color = Color(0xFFE57373))
                }
            } else {
                Button(
                    onClick = onViewMemories,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Parcourir les souvenirs", color = BackgroundPrimary)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
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
