package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    onNavigateBack: () -> Unit,
    mainViewModel: MainViewModel // On passe le MainViewModel pour le toggle vocal
) {
    val isVoiceModeActive by mainViewModel.isVoiceModeActive.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Accessibilité", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                "Mode Vocal Total",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            Text(
                "Navigue et capture tes souvenirs entièrement à la voix. Idéal si tu ne peux pas utiliser le clavier.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Activer le mode vocal", color = TextPrimary)
                Switch(
                    checked = isVoiceModeActive,
                    onCheckedChange = { mainViewModel.toggleVoiceMode() },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                )
            }
            
            if (isVoiceModeActive) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMMANDES VOCALES", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                        Text("• 'Dépose une pensée'", color = TextPrimary)
                        Text("• 'Ouvre mon fil'", color = TextPrimary)
                        Text("• 'Mode nuit'", color = TextPrimary)
                        Text("• 'Accueil'", color = TextPrimary)
                    }
                }
            }
        }
    }
}
