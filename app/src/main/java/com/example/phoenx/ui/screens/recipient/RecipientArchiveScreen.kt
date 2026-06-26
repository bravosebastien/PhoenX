package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.theme.*

/**
 * RecipientArchiveScreen (Grande Vidéothèque / Archives)
 * Affiche les photos et vidéos léguées sous forme de galerie élégante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientArchiveScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val entries by viewModel.archiveEntries.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Grande Archive", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
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
                    Text("L'album photo est encore vide.", color = TextTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        ArchiveItem(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveItem(entry: PhoenXEntry) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .phoenXMatiere(),
        contentAlignment = Alignment.Center
    ) {
        // En prod, on chargerait l'image déchiffrée ici
        // Pour le MVP, on affiche une icône et l'âge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PhotoLibrary, null, tint = AccentPrimary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${entry.ageAtCreation.years} ans",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
        
        // Petit badge de catégorie émotionnelle en overlay
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape
        ) {
            Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(
                    text = entry.aiSummary.take(10) + "...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 8.sp
                )
            }
        }
    }
}
