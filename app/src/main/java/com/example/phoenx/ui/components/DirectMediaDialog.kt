package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun DirectMediaDialog(
    type: String, // "SPOTIFY", "DEEZER", "YOUTUBE", "AUDIO"
    recipients: List<RecipientEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, userComment: String?, url: String, recipientIds: List<String>, visibility: String, autoThumbUrl: String?, includedInBook: Boolean) -> Unit,
    initialTitle: String = "",
    initialUserComment: String? = null,
    initialUrl: String = "",
    initialRecipientIds: List<String> = emptyList(),
    initialVisibility: String = "EVERYONE",
    initialIncludedInBook: Boolean = true, // v9.6.7
    onChangeCover: (() -> Unit)? = null, // v9.4.27
    onFetchMetadata: (suspend (String) -> com.example.phoenx.ui.screens.recipient.ExternalMetadata?)? = null // v9.4.27
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    var title by remember { mutableStateOf(initialTitle) }
    var userComment by remember { mutableStateOf(initialUserComment ?: "") }
    var url by remember { mutableStateOf(initialUrl) }
    var includedInBook by remember { mutableStateOf(initialIncludedInBook) } // v9.6.7
    var isFetchingMetadata by remember { mutableStateOf(false) } // v9.4.27
    var autoThumbnailUrl by remember { mutableStateOf<String?>(null) } // v9.4.27

    // AUTO-RÉCUPÉRATION DES MÉTADONNÉES (v9.4.27)
    // Uniquement pour un nouveau lien (initialUrl vide)
    LaunchedEffect(url) {
        if (initialUrl.isEmpty() && onFetchMetadata != null && url.isNotBlank() && 
            (url.contains("spotify.com") || url.contains("deezer.com") || url.contains("youtube.com") || url.contains("youtu.be"))) {
            
            isFetchingMetadata = true
            val metadata = onFetchMetadata(url)
            if (metadata != null) {
                if (title.isEmpty()) title = metadata.title ?: ""
                autoThumbnailUrl = metadata.thumbnailUrl
            }
            isFetchingMetadata = false
        }
    }
    
    // v9.4.27 : Normalisation interne UIDs -> DocIDs pour le sélecteur
    val selectedIds = remember(initialRecipientIds, recipients) {
        val docIds = initialRecipientIds.map { uidOrId ->
            recipients.find { it.linkedUid == uidOrId }?.id ?: uidOrId
        }
        mutableStateListOf<String>().apply { addAll(docIds) }
    }
    
    var visibility by remember { mutableStateOf(initialVisibility) }

    val label = when {
        type == "SPOTIFY" || url.contains("spotify") -> "un morceau Spotify"
        type == "DEEZER" || url.contains("deezer") -> "un morceau Deezer"
        type == "YOUTUBE" || url.contains("youtube") || url.contains("youtu.be") -> "une vidéo YouTube"
        type == "AUDIO" -> "ma Note Vocale"
        type == "VIDEO" -> "ma Vidéo"
        type == "PHOTO" -> "ma Photo"
        else -> "un média"
    }
    
    val placeholder = when {
        type == "DEEZER" || url.contains("deezer") -> "https://www.deezer.com/track/..."
        type == "YOUTUBE" || url.contains("youtube") -> "https://www.youtube.com/watch?v=..."
        else -> "https://open.spotify.com/track/..."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text(if (initialUrl.isEmpty() && initialTitle.isEmpty()) "Déposer $label" else "Personnaliser", color = theme.contentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // INSTRUCTIONS (v9.4.27 : Uniquement à l'ajout d'un lien externe)
                if (initialUrl.isEmpty() && (type == "SPOTIFY" || type == "DEEZER" || type == "YOUTUBE")) {
                    Surface(
                        color = accent.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Comment récupérer le lien ?", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "1. Ouvre l'app (Spotify, Deezer ou YouTube).\n2. Trouve ton contenu.\n3. Partager > Copier le lien.\n4. Reviens ici et colle-le.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // TITRE (v9.4.27 : Toujours éditable pour permettre la saisie manuelle hors-ligne)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { 
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Titre")
                            if (isFetchingMetadata) {
                                Spacer(Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = accent)
                                Spacer(Modifier.width(4.dp))
                                Text("Récupération...", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                        }
                    },
                    placeholder = { Text("Donnez un nom à ce média") },
                    modifier = Modifier.fillMaxWidth()
                )

                // COMMENTAIRE / DESCRIPTION (Masqué uniquement pour Notes Vocales - v9.4.27)
                if (type != "AUDIO" && type != "PHOENX") {
                    OutlinedTextField(
                        value = userComment,
                        onValueChange = { userComment = it },
                        label = { Text("Pourquoi ce média est important ?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                // URL (Uniquement pour liens externes type SPOTIFY/YOUTUBE)
                if (type == "SPOTIFY" || type == "DEEZER" || type == "YOUTUBE") {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Lien (URL)") },
                        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall.copy(color = theme.contentColor.copy(alpha = 0.4f))) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // BOUTON COUVERTURE (v9.4.27 : Dispo pour tous si initialisé)
                if (onChangeCover != null && (type == "AUDIO" || type == "SPOTIFY" || type == "DEEZER" || type == "PHOTO" || type == "VIDEO")) {
                    Button(
                        onClick = onChangeCover,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.1f), contentColor = accent)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.AddPhotoAlternate, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (type == "PHOTO" || type == "VIDEO") "Changer la miniature" else "Changer la photo de couverture")
                    }
                }
                
                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))

                // v9.6.7 : Contrôle granulaire pour le Livre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inclure dans mon Livre", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text("Si décoché, cette photo restera dans vos souvenirs mais ne sera pas insérée dans le manuscrit.", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = includedInBook,
                        onCheckedChange = { includedInBook = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
                    )
                }

                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))

                Text("Visibilité & Destinataires", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                RecipientSelector(
                    recipients = recipients,
                    selectedIds = selectedIds.toList(),
                    onToggleRecipient = { id ->
                        if (selectedIds.contains(id)) selectedIds.remove(id)
                        else selectedIds.add(id)
                    },
                    visibility = visibility,
                    onVisibilityChange = { visibility = it },
                    accent = accent
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    // v9.4.27 : Conversion DocIDs -> UIDs avant retour à l'appelant
                    val uids = selectedIds.map { docId ->
                        recipients.find { it.id == docId }?.linkedUid ?: docId
                    }
                    onSave(title, userComment.ifBlank { null }, url, uids, visibility, autoThumbnailUrl, includedInBook)
                },
                enabled = if (type == "AUDIO" || type == "PHOTO" || type == "VIDEO") title.isNotBlank() else url.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
