package com.example.phoenx.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCapture: (Double, Double, String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Position initiale (ex: Paris ou centre du monde)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var clickedLatLng by remember { mutableStateOf<LatLng?>(null) }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Ma Mappemonde", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    InfoPoint(
                        title = "La Mappemonde",
                        content = "Épinglez vos souvenirs là où ils sont nés. Cliquez sur un point pour revivre le moment, ou touchez la carte pour créer une nouvelle balise de vie."
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                ),
                onMapClick = { latLng ->
                    clickedLatLng = latLng
                    showAddDialog = true
                }
            ) {
                // Afficher les souvenirs existants
                if (uiState is MapUiState.Success) {
                    (uiState as MapUiState.Success).entries.forEach { entry ->
                        Marker(
                            state = MarkerState(position = LatLng(entry.latitude!!, entry.longitude!!)),
                            title = entry.locationName,
                            snippet = entry.aiSummary,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        )
                    }
                }
            }

            // Bouton d'aide flottant
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                color = BackgroundSecondary.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))
            ) {
                Text(
                    "Touchez un lieu pour y déposer un souvenir.",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        if (showAddDialog && clickedLatLng != null) {
            AddLocationDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    showAddDialog = false
                    onNavigateToCapture(clickedLatLng!!.latitude, clickedLatLng!!.longitude, name)
                }
            )
        }
    }
}

@Composable
fun AddLocationDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        title = { Text("Épingler ce lieu", color = TextPrimary) },
        text = {
            Column {
                Text("Quel souvenir associez-vous à cet endroit ?", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du lieu / Ville") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Épingler", color = BackgroundPrimary)
            }
        }
    )
}

object MapStyle {
    // Style JSON pour une carte Anthracite et Or (similaire au thème PHOEN-X)
    const val ANTHRACITE_GOLD = """
    [
      { "elementType": "geometry", "stylers": [ { "color": "#242429" } ] },
      { "elementType": "labels.text.fill", "stylers": [ { "color": "#F2EDE8" } ] },
      { "elementType": "labels.text.stroke", "stylers": [ { "color": "#242429" } ] },
      { "featureType": "administrative", "elementType": "geometry.stroke", "stylers": [ { "color": "#C97B3A" }, { "weight": 0.5 } ] },
      { "featureType": "landscape.natural", "elementType": "geometry", "stylers": [ { "color": "#2c2c31" } ] },
      { "featureType": "water", "elementType": "geometry", "stylers": [ { "color": "#1A1A1F" } ] }
    ]
    """
}
