package com.example.phoenx.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun DirectPhotoDialog(
    recipients: List<RecipientEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?, uri: Uri, recipientIds: List<String>) -> Unit
) {
    val theme = LocalAppTheme.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var visibility by remember { mutableStateOf("EVERYONE") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Déposer une photo", color = theme.contentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // SÉLECTEUR D'IMAGE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.contentColor.copy(alpha = 0.05f))
                        .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = theme.accentColor, modifier = Modifier.size(40.dp))
                            Text("Choisir une photo", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.5f))
                        }
                    }
                }

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
                onClick = { selectedUri?.let { onSave(title, description.ifBlank { null }, it, selectedIds.toList()) } },
                enabled = selectedUri != null
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
