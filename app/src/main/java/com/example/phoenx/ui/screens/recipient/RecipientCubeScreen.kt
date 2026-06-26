package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * RecipientCubeScreen (Signature PHOEN-X 5.0)
 * L'interface immersive pour les Destinataires.
 * C'est ici que l'armoire Spline 3D sera affichée.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientCubeScreen(
    onExit: () -> Unit
) {
    Scaffold(
        containerColor = Color.Black, // Fond noir pur pour l'immersion 3D
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("L'Armoire de [Nom]", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                        Text("Explore son héritage", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Aide Jeu de Piste */ }) {
                        Icon(Icons.Default.HelpOutline, null, tint = AccentPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ZONE 3D SPLINE (Placeholder en attendant ton fichier)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "L'Armoire Interactive",
                        style = MaterialTheme.typography.displayMedium,
                        color = AccentPrimary.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Prête à accueillir ton design Spline 3D.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // NAVIGATION RAPIDE DES TIROIRS (Overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Touche un tiroir ou un livre pour commencer l'exploration.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DrawerShortCut("Bibliothèque", "Textes")
                    DrawerShortCut("Discothèque", "Audio")
                    DrawerShortCut("Archives", "Photos")
                }
            }
        }
    }
}

@Composable
fun DrawerShortCut(label: String, subtitle: String) {
    Surface(
        color = SurfaceCard.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.2f)),
        modifier = Modifier.size(100.dp, 80.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontSize = 10.sp)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AccentPrimary, fontSize = 9.sp)
        }
    }
}
