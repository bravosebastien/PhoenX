package com.example.phoenx.ui.screens.genealogy

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.result.PickVisualMediaRequest
import coil3.compose.AsyncImage
import dagger.hilt.android.EntryPointAccessors
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.ui.components.CameoPortrait
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.Error
import com.example.phoenx.ui.theme.LocalAppTheme
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailsDialog(
    person: PersonEntity,
    viewModel: GenealogyTreeViewModel,
    onDismiss: () -> Unit,
    accent: Color,
    navController: androidx.navigation.NavController, // v9.6.7
    isReadOnly: Boolean = false,
    onEditLinks: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    
    // v9.6.7 : Mémorisation du flux pour garantir la stabilité et la réactivité Room
    val mediaFlow = remember(person.id) { viewModel.getMediaForPerson(person.id) }
    val mediaList by mediaFlow.collectAsState(initial = emptyList())
    val resolvedUrls by viewModel.resolvedUrls.collectAsState()
    
    var biography by remember(person) { mutableStateOf(person.biography) }
    var isDeceased by remember(person) { mutableStateOf(person.isDeceased) }
    var relationLabel by remember(person) { mutableStateOf(person.reparentedRelationLabel ?: "") }
    var profilePhotoPath by remember(person) { mutableStateOf(person.imagePath) }
    var showDeleteConfirm by remember { mutableStateOf(false) } // v9.4.22
    val children by viewModel.getChildrenOf(person.id).collectAsState(initial = emptyList())
    val deleteRelationLabels = remember { mutableStateMapOf<String, String>() }
    var videoErrorMessage by remember { mutableStateOf<String?>(null) } // v9.6.6

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri)
            val type = if (mime?.contains("video") == true) "VIDEO" else "PHOTO"

            if (type == "VIDEO") {
                val isValid = com.example.phoenx.ui.util.VideoUtils.isVideoDurationValid(
                    context, uri, com.example.phoenx.ui.util.VideoUtils.MAX_VIDEO_DURATION_SECONDS_STANDARD
                )
                if (isValid) {
                    val file = viewModel.uriToFile(uri)
                    if (file != null) {
                        viewModel.addMedia(person.id, file, "VIDEO")
                        videoErrorMessage = null
                    }
                } else {
                    videoErrorMessage = "Cette vidéo dépasse la durée maximale de 90 secondes autorisée. Merci de choisir une vidéo plus courte."
                }
            } else {
                val file = viewModel.uriToFile(uri)
                if (file != null) {
                    viewModel.addMedia(person.id, file, "PHOTO")
                    videoErrorMessage = null
                }
            }
        }
    }

    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = "person_profile_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            profilePhotoPath = file.absolutePath
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // CONTENU SCROLLABLE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // PORTRAIT UNIFIÉ (Inspiré des Rencontres, format moyen haut-gauche)
                        Box(
                            modifier = Modifier
                                .size(110.dp, 140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.contentColor.copy(alpha = 0.05f))
                                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isReadOnly) { profilePhotoPicker.launch("image/*") }
                        ) {
                            val activePath = profilePhotoPath ?: person.imagePath
                            if (activePath != null) {
                                val isPathEncrypted = activePath.endsWith(".enc")
                                com.example.phoenx.ui.components.SecureAsyncImage(
                                    mediaUrl = activePath,
                                    mediaManager = EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager(),
                                    isEncrypted = isPathEncrypted,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person, 
                                    null, 
                                    modifier = Modifier.size(48.dp).align(Alignment.Center).alpha(0.2f), 
                                    tint = theme.contentColor
                                )
                            }
                            
                            if (!isReadOnly) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(28.dp)
                                        .background(accent, CircleShape)
                                        .border(2.dp, theme.backgroundColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, tint = theme.backgroundColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(20.dp))
                        
                        // IDENTITÉ ET ACTIONS RAPIDES (Style inspiré des Rencontres)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = person.firstName, 
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold, 
                                    fontFamily = theme.fontFamily
                                ), 
                                color = theme.contentColor
                            )
                            
                            if (!isReadOnly) {
                                // Case "Décédé(e)" (Spécifique Arbre)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = isDeceased,
                                        onCheckedChange = { isDeceased = it },
                                        colors = CheckboxDefaults.colors(checkedColor = accent)
                                    )
                                    Text("Décédé(e)", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                                }
                                
                                // Bouton de liens (Style bouton d'action Rencontres)
                                OutlinedButton(
                                    onClick = onEditLinks,
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor)
                                ) {
                                    Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Identité & Liens",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else if (person.isDeceased) {
                                // Badge Décédé en mode lecture
                                Surface(
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = theme.contentColor.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        "Décédé(e)", 
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                                        color = theme.contentColor.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (person.isReparented) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = relationLabel,
                            onValueChange = { relationLabel = it },
                            label = { Text("Nature du lien (ex: Petit-fils)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    Text("BIOGRAPHIE", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                    if (!isReadOnly) {
                        OutlinedTextField(
                            value = biography,
                            onValueChange = { biography = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            placeholder = { Text("Quelques mots sur sa vie...", fontStyle = FontStyle.Italic) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
                        ) {
                            Text(
                                text = biography.ifBlank { "Aucune biographie renseignée." },
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                                color = theme.contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GALERIE MÉDIA", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.4f))
                            InfoButton(
                                title = "Photo de profil vs Galerie",
                                points = listOf("La photo de profil est celle affichée dans l'arbre. La galerie peut contenir d'autres photos et vidéos, visibles uniquement en ouvrant le détail de cette personne.")
                            )
                        }
                        if (!isReadOnly) {
                            IconButton(onClick = { 
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) 
                            }) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = accent)
                            }
                        }
                    }

                    if (videoErrorMessage != null) {
                        Text(
                            text = videoErrorMessage!!,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    if (!isReadOnly) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            color = accent.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Ce média sera visible par TOUS vos Destinataires une fois l'héritage activé, sans restriction possible.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.contentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (mediaList.isEmpty()) {
                        Text("Aucun média ajouté.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        mediaList.chunked(3).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { media ->
                                    val activeUrl = media.thumbnailPath ?: media.mediaPath
                                    val cacheKey = if (media.thumbnailPath != null) "thumb_${media.id}" else media.id
                                    val resolvedUrl = resolvedUrls[cacheKey]

                                    val isPathEncrypted = activeUrl.endsWith(".enc")
                                    val isLocal = activeUrl.startsWith("/") || activeUrl.startsWith("file://")
                                    val cleanLocalPath = if (activeUrl.startsWith("file://")) activeUrl.substring(7) else if (activeUrl.startsWith("/")) activeUrl else null
                                    val fieldParam = if (media.thumbnailPath != null) "thumbnailPath" else "mediaPath"

                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navController.navigate(
                                                    com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(
                                                        entryId = media.id,
                                                        creatorId = null,
                                                        mediaUrl = media.mediaPath,
                                                        entryType = media.mediaType,
                                                        aiSummary = "Média de ${person.firstName}",
                                                        sourceDocType = "personMedia",
                                                        personId = person.id,
                                                        isEncrypted = media.mediaPath.endsWith(".enc")
                                                    )
                                                )
                                            }
                                    ) {
                                        // v9.6.7 : Harmonisation complète avec la photo de profil (CameoPortrait)
                                        // On laisse SecureAsyncImage gérer le fallback local via mediaUrl (file://)
                                        val bestUrl = resolvedUrl ?: (if (activeUrl.startsWith("/")) "file://$activeUrl" else activeUrl)
                                        
                                        com.example.phoenx.ui.components.SecureAsyncImage(
                                            mediaUrl = bestUrl,
                                            mediaManager = EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager(),
                                            isEncrypted = isPathEncrypted,
                                            docType = "personMedia",
                                            docId = media.id,
                                            field = fieldParam,
                                            personId = person.id,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        if (media.mediaType == "VIDEO") {
                                            Icon(
                                                Icons.Default.PlayCircle,
                                                null,
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(24.dp).align(Alignment.Center)
                                            )
                                        }

                                        if (!isReadOnly) {
                                            IconButton(
                                                onClick = { viewModel.removeMedia(media) },
                                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }

                // BARRE D'ACTIONS FIXE (STICKY)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!isReadOnly) {
                            Button(
                                onClick = { 
                                    viewModel.savePersonDetails(person.id, biography, relationLabel.ifBlank { null }, isDeceased, profilePhotoPath)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Enregistrer les modifications", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(12.dp))

                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp), tint = Error)
                                Spacer(Modifier.width(8.dp))
                                Text("Supprimer de l'arbre", color = Error, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.contentColor.copy(alpha = 0.05f), contentColor = theme.contentColor)
                            ) { Text("Fermer") }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = theme.backgroundColor,
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Supprimer cette personne ?", color = theme.contentColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    InfoButton(
                        title = "Continuité des liens",
                        points = listOf("Si cette personne a des parents renseignés, ses enfants seront automatiquement rattachés à eux plutôt que de perdre tout lien. Vous pourrez préciser vous-même la nature de ce nouveau lien juste après.")
                    )
                }
            },
            text = {
                Column {
                    Text("Cette action est irréversible. Elle sera retirée de votre répertoire, de l'arbre généalogique et tous ses médias seront effacés.", color = theme.contentColor.copy(alpha = 0.7f))
                    
                    if (children.isNotEmpty() && !person.parentIds.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Les enfants suivants seront rattachés directement à leurs grands-parents. Comment décrire ce nouveau lien ?", style = MaterialTheme.typography.labelSmall, color = accent)
                        
                        children.forEach { child ->
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = deleteRelationLabels[child.id] ?: "",
                                onValueChange = { deleteRelationLabels[child.id] = it },
                                label = { Text("Lien pour ${child.firstName}") },
                                placeholder = { Text("ex: Petit-fils") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePerson(person.id, deleteRelationLabels.toMap())
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("Supprimer définitivement", color = Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler", color = theme.contentColor) }
            }
        )
    }
}


