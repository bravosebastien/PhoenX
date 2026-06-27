package com.example.phoenx.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.ui.theme.*

@Composable
fun BookItem(entry: PhoenXEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .phoenXMatiere(isPaper = true),
        colors = CardDefaults.cardColors(containerColor = MateriauPapier.copy(alpha = 0.9f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoStories, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "À ${entry.ageAtCreation.years} ans",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = entry.aiSummary.ifEmpty { "Fragment de pensée..." },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF24211F)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = String(entry.encryptedContent),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF4A4542)
                ),
                maxLines = 5,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
