package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onNavigateToPermissions: (String) -> Unit,
    viewModel: RecipientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recipients = (uiState as? RecipientUiState.Success)?.recipients ?: emptyList()
    val recipient = recipients.find { it.id == recipientId }
    
    // Charger le portrait et les souvenirs liés
    val portraitEntry by viewModel.getPortraitForRecipient(recipientId).collectAsState(initial = null)
    val linkedEntries by viewModel.getEntriesForRecipient(recipientId).collectAsState(initial = emptyList())
    
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { Text(recipient?.name ?: "Détails", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (recipient == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header Profil
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = accent.copy(alpha = 0.1f)
                    ) {
                        Icon(Icons.Default.Person, null, tint = accent, modifier = Modifier.padding(16.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(recipient.name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text(recipient.relationship, style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ÉTAT DU PORTRAIT
                Text("SON PORTRAIT (LE MIROIR)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    onClick = { onComposePortrait(recipient.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.4f)),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (portraitEntry != null) Success.copy(alpha = 0.3f) else accent.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (portraitEntry != null) Icons.Default.CheckCircle else Icons.Default.HistoryEdu,
                            contentDescription = null,
                            tint = if (portraitEntry != null) Success else accent
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (portraitEntry != null) "Portrait complété" else "Portrait non commencé",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (portraitEntry != null) "Clique pour modifier tes mots." else "Dis-lui ce que tu vois en lui/elle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("CONTENUS LIÉS (${linkedEntries.filter { it.entryType != "PORTRAIT" }.size})", style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))

                if (linkedEntries.none { it.entryType != "PORTRAIT" }) {
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
                                "Aucun souvenir n'est encore destiné à ${recipient.name}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    linkedEntries.filter { it.entryType != "PORTRAIT" }.forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mail, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(entry.aiSummary.ifEmpty { "Souvenir" }, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onNavigateToPermissions(recipient.id) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Security, null, tint = accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Gérer les droits d'accès", color = TextPrimary)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
