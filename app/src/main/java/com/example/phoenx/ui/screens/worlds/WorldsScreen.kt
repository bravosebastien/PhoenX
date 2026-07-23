package com.example.phoenx.ui.screens.worlds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorld: (String) -> Unit,
    viewModel: WorldsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Les Tiroirs de ma Vie", style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                        Text("Rangement assisté par IA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            when (val state = uiState) {
                is WorldsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
                }
                is WorldsUiState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = theme.contentColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("La commode est vide.", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f))
                        Text("Capture des souvenirs pour les voir se ranger ici.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.4f))
                    }
                }
                is WorldsUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.worlds) { world ->
                            WorldDrawerItem(world, theme, onClick = { onNavigateToWorld(world.name) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldDrawerItem(world: WorldItem, theme: AppThemeState, onClick: () -> Unit) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick)
            .phoenXMatiere(isPaper = false), // Texture Cuir/Bois
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Default.AutoAwesome, null, tint = accent.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                Text(text = "${world.count}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = world.name.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                color = theme.contentColor
            )
            
            if (world.lastEntrySummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = world.lastEntrySummary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                    color = theme.contentColor.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
