package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun InfoPoint(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Aide",
            tint = accent.copy(alpha = 0.6f)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = theme.backgroundColor,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, color = theme.contentColor, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Text(
                    content,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("J'ai compris", color = accent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
