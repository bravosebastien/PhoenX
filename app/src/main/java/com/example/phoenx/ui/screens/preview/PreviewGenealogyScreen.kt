package com.example.phoenx.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewGenealogyScreen(
    recipientUid: String,
    onNavigateBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current

    LaunchedEffect(recipientUid) {
        viewModel.loadPreview(recipientUid)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Arbre Généalogique (Aperçu)", style = MaterialTheme.typography.labelSmall, color = accent)
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
                Icon(Icons.Default.Public, null, modifier = Modifier.size(64.dp), tint = accent.copy(alpha = 0.2f))
                Spacer(Modifier.height(24.dp))
                Text(
                    "Consultation de l'Arbre",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Votre destinataire pourra consulter l'intégralité de l'arbre généalogique que vous avez construit.\n\nNombre de membres visibles : ${state.familyCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
