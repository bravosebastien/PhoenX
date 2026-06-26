package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientDiscothequeScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val entries by viewModel.discothequeEntries.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Grande Discothèque", style = MaterialTheme.typography.displaySmall) },
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
                    Text("Le tourne-disque est silencieux pour le moment.", color = TextTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                        VinylItem(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun VinylItem(entry: PhoenXEntry) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFF121215)) // Pochette noire
                .phoenXMatiere(),
            contentAlignment = Alignment.Center
        ) {
            // Le disque (Cercle au centre)
            Surface(
                modifier = Modifier.fillMaxSize(0.85f),
                shape = CircleShape,
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.DarkGray.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Étiquette centrale (Braise)
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = AccentPrimary
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = BackgroundPrimary, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            
            // Overlay Play
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "À ${entry.ageAtCreation.years} ans",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary
        )
        Text(
            text = entry.aiSummary.ifEmpty { "Souvenir vocal" },
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
