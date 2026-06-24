package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Protocole d'activation", style = MaterialTheme.typography.labelLarge) },
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
                "Gère ton héritage numérique",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("STATUT", style = MaterialTheme.typography.labelSmall, color = Success)
                    Text("Dormant (Tout va bien)", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = "Fab", 
                onValueChange = {},
                label = { Text("Nom du Dépositaire") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("DÉLAI DE CONTESTATION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            var sliderPos by remember { mutableFloatStateOf(72f) }
            Slider(
                value = sliderPos,
                onValueChange = { sliderPos = it },
                valueRange = 24f..72f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = AccentPrimary, activeTrackColor = AccentPrimary)
            )
            Text("${sliderPos.toInt()} heures", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "ATTENTION : Ce protocole ne remplace pas un testament et n'a aucune valeur légale. Il s'agit d'un engagement moral et d'une transmission privée.",
                style = MaterialTheme.typography.labelSmall,
                color = Warning,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Save */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Enregistrer les réglages", color = BackgroundPrimary)
            }
        }
    }
}
