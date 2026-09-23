package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.phoenx.R
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
    // v12.3 : Clé de mémorisation étendue à la liste entière pour réagir au chargement asynchrone
    val selectedIds = remember(initialRecipientIds, recipients) {
        val docIds = initialRecipientIds.map { uidOrId ->
            recipients.find { it.linkedUid == uidOrId }?.id ?: uidOrId
        }
        mutableStateListOf<String>().apply { addAll(docIds) }
    }
    
    var visibility by remember { mutableStateOf(initialVisibility) }

    val label = when {
        type == "SPOTIFY" || url.contains("spotify") -> stringResource(R.string.dialog_media_label_spotify)
        type == "DEEZER" || url.contains("deezer") -> stringResource(R.string.dialog_media_label_deezer)
        type == "YOUTUBE" || url.contains("youtube") || url.contains("youtu.be") -> stringResource(R.string.dialog_media_label_youtube)
        type == "AUDIO" -> stringResource(R.string.dialog_media_label_audio)
        type == "VIDEO" -> stringResource(R.string.dialog_media_label_video)
        type == "PHOTO" -> stringResource(R.string.dialog_media_label_photo)
        else -> stringResource(R.string.dialog_media_label_default)
    }
    
    val placeholder = when {
        type == "DEEZER" || url.contains("deezer") -> "https://www.deezer.com/track/..."
        type == "YOUTUBE" || url.contains("youtube") -> "https://www.youtube.com/watch?v=..."
        else -> "https://open.spotify.com/track/..."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text(if (initialUrl.isEmpty() && initialTitle.isEmpty()) stringResource(R.string.dialog_media_title_deposit, label) else stringResource(R.string.dialog_media_title_customize), color = theme.contentColor) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // INSTRUCTIONS (v9.4.27 : Uniquement à l'ajout d'un lien externe)
                if (initialUrl.isEmpty() && (type == "SPOTIFY" || type == "DEEZER" || type == "YOUTUBE")) {
                    Surface(
                        color = accent.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.dialog_media_howto_title), style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.dialog_media_howto_steps),
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
                            Text(stringResource(R.string.dialog_media_label_title))
                            if (isFetchingMetadata) {
                                Spacer(Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = accent)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.dialog_media_fetching), style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                        }
                    },
                    placeholder = { Text(stringResource(R.string.dialog_media_placeholder_title)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // COMMENTAIRE / DESCRIPTION (Masqué uniquement pour Notes Vocales - v9.4.27)
                if (type != "AUDIO" && type != "PHOENX") {
                    OutlinedTextField(
                        value = userComment,
                        onValueChange = { userComment = it },
                        label = { Text(stringResource(R.string.dialog_media_label_importance)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                // URL (Uniquement pour liens externes type SPOTIFY/YOUTUBE)
                if (type == "SPOTIFY" || type == "DEEZER" || type == "YOUTUBE") {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.dialog_media_label_url)) },
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
                        Text(if (type == "PHOTO" || type == "VIDEO") stringResource(R.string.dialog_media_btn_change_thumb) else stringResource(R.string.dialog_media_btn_change_cover))
                    }
                }
                
                HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))

                // v9.6.7 : Contrôle granulaire pour le Livre (Uniquement pour PHOTOS réelles - v12.2)
                if (type == "PHOTO") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.dialog_media_label_include_book), style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.dialog_media_desc_include_book), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = includedInBook,
                            onCheckedChange = { includedInBook = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = accent)
                        )
                    }

                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))
                }

                Text(stringResource(R.string.dialog_media_visibility_destinataires), style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
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
                        val rec = recipients.find { it.id == docId }
                        val finalId = rec?.linkedUid ?: docId
                        android.util.Log.d("PHOENX_SHARE_DIAG", "Mapping recipient: name=${rec?.name}, docId=$docId, linkedUid=${rec?.linkedUid} -> finalId=$finalId")
                        finalId
                    }
                    val finalIncludedInBook = if (type == "PHOTO") includedInBook else false
                    onSave(title, userComment.ifBlank { null }, url, uids, visibility, autoThumbnailUrl, finalIncludedInBook)
                },
                enabled = if (type == "AUDIO" || type == "PHOTO" || type == "VIDEO") title.isNotBlank() else url.isNotBlank()
            ) {
                Text(stringResource(R.string.dialog_media_btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_media_btn_cancel))
            }
        }
    )
}
