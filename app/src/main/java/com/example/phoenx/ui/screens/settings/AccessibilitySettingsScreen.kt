package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    onNavigateBack: () -> Unit,
    mainViewModel: MainViewModel
) {
    val isVoiceActive by mainViewModel.isVoiceModeActive.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Accessibilité", style = MaterialTheme.typography.labelLarge) },
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
            Brush.radialGradient(listOf(BackgroundSecondary, BackgroundPrimary), radius = 2000f)
        )) {
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
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Conçu pour naviguer, écouter et capturer vos souvenirs sans utiliser le clavier. Idéal pour un usage mains-libres ou pour faciliter l'utilisation par nos aînés.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Surface(
                    color = SurfaceCard,
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isVoiceActive) AccentPrimary else TextTertiary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver, 
                            null, 
                            tint = if (isVoiceActive) AccentPrimary else TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Activer la voix", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (isVoiceActive) "L'application vous écoute" else "Désactivé",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isVoiceActive) AccentPrimary else TextTertiary
                            )
                        }
                        Switch(
                            checked = isVoiceActive,
                            onCheckedChange = { mainViewModel.toggleVoiceMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentPrimary,
                                checkedTrackColor = AccentPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (isVoiceActive) {
                    Text(
                        "Commandes disponibles :",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val commands = listOf(
                        "'Dépose une pensée'",
                        "'Ouvre mon Fil de Pensée'",
                        "'Mode Nuit'",
                        "'Retour à l'accueil'",
                        "'Aide'"
                    )
                    
                    commands.forEach { cmd ->
                        Text(
                            text = "• $cmd",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
