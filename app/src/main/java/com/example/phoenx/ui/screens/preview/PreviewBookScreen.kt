package com.example.phoenx.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewBookScreen(
    recipientUid: String,
    onNavigateBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current

    // TODO: Charger les chapitres du livre via un service ou ViewModel dédié
    // Pour l'étape 5, on affiche une interface de consultation
    
    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Livre de Vie (Aperçu)", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(state.recipientName, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(64.dp), tint = accent.copy(alpha = 0.2f))
                Spacer(Modifier.height(24.dp))
                Text(
                    "Consultation du manuscrit",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Le Livre de Vie est généré à partir de tous les souvenirs marqués 'Inclure dans mon Livre'.\n\nNote : Tout ce qui est inclus ici sera visible par tous vos destinataires.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
