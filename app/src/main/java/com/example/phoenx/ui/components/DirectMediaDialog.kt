package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun DirectMediaDialog(
    type: String, // "SPOTIFY" or "YOUTUBE"
    recipients: List<RecipientEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?, url: String, recipientIds: List<String>) -> Unit,
    initialTitle: String = "",
    initialDescription: String? = null,
    initialUrl: String = "",
    initialRecipientIds: List<String> = emptyList(),
    initialVisibility: String = "EVERYONE"
) {
    val theme = LocalAppTheme.current
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    var url by remember { mutableStateOf(initialUrl) }
    val selectedIds = remember { mutableStateListOf<String>().apply { addAll(initialRecipientIds) } }
    var visibility by remember { mutableStateOf(initialVisibility) }

    val label = if (type == "SPOTIFY") "un morceau Spotify" else "une vidéo YouTube"
    val placeholder = if (type == "SPOTIFY") "https://open.spotify.com/track/..." else "https://www.youtube.com/watch?v=..."

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Déposer $label", color = theme.contentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnelle)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Lien (URL)") },
                    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall.copy(color = theme.contentColor.copy(alpha = 0.4f))) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Visibilité", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                RecipientSelector(
                    recipients = recipients,
                    selectedIds = selectedIds,
                    visibility = visibility,
                    onVisibilityChange = { visibility = it },
                    accent = theme.accentColor
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description.ifBlank { null }, url, selectedIds.toList()) },
                enabled = url.isNotBlank()
            ) {
                Text("Sauvegarder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
