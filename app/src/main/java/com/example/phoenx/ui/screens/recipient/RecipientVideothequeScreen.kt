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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientVideothequeScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel(),
) {
    val entries by viewModel.videoEntries.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = { Text("Grande Vidéothèque", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCapture) {
                        Icon(Icons.Default.Add, null, tint = accent)
                    }
                    InfoPoint(
                        title = "La Vidéothèque",
                        content = "Vos souvenirs les plus vivants sont conservés ici sous forme de cassettes VHS. Cliquez sur une cassette pour voir le souvenir."
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Le projecteur est éteint pour le moment.", color = theme.contentColor.copy(alpha = 0.4f), textAlign = TextAlign.Center)
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
                        VHSCard(entry, theme) { onNavigateToDetail(entry.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun VHSCard(entry: PhoenXEntry, theme: AppThemeState, onClick: () -> Unit) {
    val accent = theme.accentColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFF1A1A2A)) // Corps VHS (fixe pour le style cassette)
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
                color = theme.contentColor.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(entry.aiSummary.ifEmpty { "Souvenir vidéo" }, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black, maxLines = 1)
                }
            }

            // Bouton Play invisible (uniquement l'icône)
            Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "À ${entry.ageAtCreation.years} ans",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
    }
}
