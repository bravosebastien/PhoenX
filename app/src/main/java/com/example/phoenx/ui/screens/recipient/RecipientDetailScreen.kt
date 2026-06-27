package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientDetailScreen(
    recipientId: String,
    onNavigateBack: () -> Unit,
    onComposePortrait: (String) -> Unit,
    viewModel: RecipientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recipients = (uiState as? RecipientUiState.Success)?.recipients ?: emptyList()
    val recipient = recipients.find { it.id == recipientId }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text(recipient?.name ?: "Détails", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
        }
    ) { padding ->
        if (recipient == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(BackgroundSecondary, BackgroundPrimary))
            )) {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    // Header Profil
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = AccentPrimary.copy(alpha = 0.1f)
                        ) {
                            Icon(Icons.Default.Person, null, tint = AccentPrimary, modifier = Modifier.padding(16.dp))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(recipient.name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                            Text(recipient.relationship, style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text("CONTENUS PRÉPARÉS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Ici on filtrera les OfflineEntries qui ont recipientId dans leur liste
                    // Pour le MVP on affiche un placeholder
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceCard.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.large,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoStories, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Tous les souvenirs taggués pour ${recipient.name} seront regroupés ici.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onComposePortrait(recipient.id) },
                        modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(Icons.Default.Person, null, tint = BackgroundPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Composer son Portrait", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
