package com.example.phoenx.ui.screens.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.phoenXMatiere
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttentionsScreen(
    viewModel: AttentionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else if (uiState.links.isEmpty()) {
            EmptyAttentionsContent(accent, theme)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Dernières attentions reçues",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(uiState.links) { link ->
                    AttentionCard(link, accent, theme)
                }
            }
        }
    }
}

@Composable
fun AttentionCard(link: LivingLinkUiModel, accent: Color, theme: com.example.phoenx.ui.theme.AppThemeState) {
    Card(
        modifier = Modifier.fillMaxWidth().phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.MailOutline, null, tint = accent, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${link.creatorName} vous a écrit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(link.sentAt))
                    Text(text = "Reçu le $dateStr", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.5f))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = link.decryptedText ?: "Contenu chiffré",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
            
            // TODO: Affichage des médias si présents
        }
    }
}

@Composable
fun EmptyAttentionsContent(accent: Color, theme: com.example.phoenx.ui.theme.AppThemeState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.VolunteerActivism, 
            null, 
            modifier = Modifier.size(64.dp), 
            tint = accent.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Aucune attention pour le moment.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "C'est ici que vous recevrez les souvenirs que vos proches choisissent de vous transmettre de leur vivant.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
