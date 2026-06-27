package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientVideothequeScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val entries by viewModel.videoEntries.collectAsState()
    var selectedVideo by remember { mutableStateOf<PhoenXEntry?>(null) }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Grande Vidéothèque", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    InfoPoint(
                        title = "La Vidéothèque",
                        content = "Vos souvenirs les plus vivants sont conservés ici sous forme de cassettes VHS. Cliquez sur une cassette pour lancer le projecteur."
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(BackgroundSecondary, BackgroundPrimary))
        )) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Le projecteur est éteint pour le moment.", color = TextTertiary, textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(entries) { entry ->
                        VHSCard(entry, onClick = { selectedVideo = entry })
                    }
                }
            }

            // Lecteur Vidéo Immersif Overlay
            if (selectedVideo != null) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simulation Lecteur Vidéo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16/9f)
                                .background(Color.DarkGray)
                                .border(1.dp, AccentPrimary.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayCircle, null, tint = AccentPrimary, modifier = Modifier.size(64.dp))
                            Text(
                                "Chargement du souvenir...", 
                                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = selectedVideo!!.aiSummary,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Enregistré à ${selectedVideo!!.ageAtCreation.years} ans",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                    }

                    IconButton(
                        onClick = { selectedVideo = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VHSCard(entry: PhoenXEntry, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFF1A1A2A)) // Corps VHS
                .clickable(onClick = onClick)
                .phoenXMatiere(),
            contentAlignment = Alignment.Center
        ) {
            // Fenêtre de la cassette
            Surface(
                modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.4f).align(Alignment.TopCenter).padding(top = 12.dp),
                color = Color.Black.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    repeat(2) {
                        Surface(modifier = Modifier.size(24.dp), shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFF2E2E35)) {}
                    }
                }
            }
            
            // Étiquette (Braise)
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).align(Alignment.BottomCenter).padding(bottom = 8.dp),
                color = TextPrimary.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(entry.aiSummary.ifEmpty { "Souvenir vidéo" }, style = MaterialTheme.typography.labelSmall, color = Color.Black, maxLines = 1)
                }
            }

            // Bouton Play invisible (uniquement l'icône)
            Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "À ${entry.ageAtCreation.years} ans",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary
        )
    }
}
