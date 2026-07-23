package com.example.phoenx.ui.screens.detective

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectiveHomeScreen(
    navController: NavController,
    viewModel: DetectiveHomeViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mode Détective",
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                                color = theme.contentColor
                            )
                            Text(
                                text = "Cache un souvenir derrière une question",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.6f)
                            )
                        }
                        InfoButton(
                            title = "Mode Détective",
                            points = listOf(
                                "Cache un contenu derrière une question secrète.",
                                "La réponse est protégée localement par SHA-256.",
                                "Ton proche a plusieurs tentatives pour trouver.",
                                "Tape sur 'Besoin d'inspiration' pour des exemples.",
                                "Différent du Tiroir à Clé — pas de limite d'ouvertures."
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("detective_create") },
                containerColor = accent,
                contentColor = theme.backgroundColor
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer une énigme")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else if (entries.isEmpty()) {
            EmptyDetectiveState(onAddClick = { navController.navigate("detective_create") }, theme = theme)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(entries) { entry ->
                    DetectiveEnigmaCard(entry, theme)
                }
            }
        }
    }
}

@Composable
fun EmptyDetectiveState(onAddClick: () -> Unit, theme: AppThemeState) {
    val accent = theme.accentColor
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = accent
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Aucune énigme créée pour l'instant.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = theme.fontFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold
            ),
            color = theme.contentColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Crée ton premier mystère pour un proche.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            modifier = Modifier.phoenXMatiere(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("+ Créer une énigme", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetectiveEnigmaCard(entry: OfflineEntry, theme: AppThemeState) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.enigmaQuestion ?: "Énigme sans question",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Text(
                    text = "Pour : ${entry.recipientIds.ifEmpty { "Tes proches" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
            Surface(
                color = accent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = entry.entryType,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
            }
        }
    }
}
