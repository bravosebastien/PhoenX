package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeirAllocationScreen(
    recipientId: String,
    navController: NavController,
    viewModel: RecipientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recipients = (uiState as? RecipientUiState.Success)?.recipients ?: emptyList()
    val recipient = recipients.find { it.id == recipientId }
    
    val entries by viewModel.getEntriesForRecipientUnified(recipientId).collectAsState(initial = emptyList())
    
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Fiche Héritier", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(recipient?.name ?: "Détails", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "${entries.size} souvenirs sont destinés à ${recipient?.name ?: "ce proche"}.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(entries) { entry ->
                    AllocationEntryRow(
                        entry = entry,
                        onClick = { navController.navigate(Screen.MemoryDetail.createRoute(entry.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun AllocationEntryRow(
    entry: OfflineEntry,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
    val formattedDate = sdf.format(Date(entry.createdAt))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icône Type
            val icon = when(entry.entryType) {
                "PHOTO", "GALLERY" -> Icons.Default.PhotoCamera
                "AUDIO" -> Icons.Default.Mic
                "VIDEO" -> Icons.Default.Videocam
                "PORTRAIT" -> Icons.Outlined.AccountCircle
                "QUESTION_ANSWER" -> Icons.AutoMirrored.Filled.HelpOutline
                else -> Icons.Default.Description
            }
            
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = SurfaceCard.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val title = when(entry.entryType) {
                    "PORTRAIT" -> entry.aiSummary
                    "QUESTION_ANSWER" -> "Ma réponse à : ${entry.aiSummary}"
                    else -> entry.aiSummary.ifEmpty { "Souvenir sans titre" }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
            
            if (entry.visibility == "EVERYONE") {
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "PUBLIC", 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = accent
                    )
                }
            }
        }
    }
}
