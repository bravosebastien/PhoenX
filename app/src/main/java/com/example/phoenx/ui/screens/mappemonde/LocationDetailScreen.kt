package com.example.phoenx.ui.screens.mappemonde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    locationId: String,
    navController: NavController,
    mode: MapMode = MapMode.CREATOR,
    viewModel: LocationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                        Text("${loc.emoji} ${loc.placeName}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        when (uiState) {
            is LocationDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            }
            is LocationDetailUiState.Success -> {
                val data = uiState as LocationDetailUiState.Success
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Mini Carte statique
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(LatLng(data.location.latitude, data.location.longitude), 12f)
                            },
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                scrollGesturesEnabled = false,
                                zoomGesturesEnabled = false,
                                tiltGesturesEnabled = false,
                                rotationGesturesEnabled = false
                            )
                        ) {
                            Marker(state = MarkerState(position = LatLng(data.location.latitude, data.location.longitude)))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "SOUVENIRS ÉPINGLÉS",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        if (data.entries.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Aucun souvenir n'a encore été attaché à ce lieu.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(data.entries) { entry ->
                                MemoryCard(entry)
                            }
                        }
                    }

                    if (mode == MapMode.CREATOR) {
                        Button(
                            onClick = { navController.navigate("capture?locationId=$locationId") },
                            modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajouter un souvenir ici", color = BackgroundPrimary)
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun MemoryCard(entry: OfflineEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, null, tint = AccentPrimary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(entry.aiSummary, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif), color = TextPrimary)
                Text("Enregistré le ...", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }
    }
}
