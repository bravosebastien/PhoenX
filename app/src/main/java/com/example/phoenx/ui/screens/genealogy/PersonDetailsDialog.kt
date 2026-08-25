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
import coil3.compose.AsyncImage
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
    isReadOnly: Boolean = false,
    onEditLinks: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val mediaList by viewModel.getMediaForPerson(person.id).collectAsState(initial = emptyList())
    val resolvedUrls by viewModel.resolvedUrls.collectAsState()
    
    LaunchedEffect(mediaList) {
        mediaList.forEach { media ->
            viewModel.resolveMediaUrl(person.id, media)
        }
    }
    
    var biography by remember(person) { mutableStateOf(person.biography) }
    var isDeceased by remember(person) { mutableStateOf(person.isDeceased) }
    var relationLabel by remember(person) { mutableStateOf(person.reparentedRelationLabel ?: "") }
    var profilePhotoPath by remember(person) { mutableStateOf(person.imagePath) }
    var showDeleteConfirm by remember { mutableStateOf(false) } // v9.4.22
    val children by viewModel.getChildrenOf(person.id).collectAsState(initial = emptyList())
    val deleteRelationLabels = remember { mutableStateMapOf<String, String>() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = "person_media_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            viewModel.addMedia(person.id, file.absolutePath, "PHOTO")
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            CameoPortrait(
                                imagePath = profilePhotoPath, 
                                firstName = person.firstName, 
                                size = 80.dp,
                                resolvedUrl = if (profilePhotoPath == person.imagePath) resolvedUrls[person.id] else null,
                                useCharcoalFilter = false // v9.4.29 : Couleurs naturelles dans la fiche détail
                            )
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = { profilePhotoPicker.launch("image/*") },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(accent, CircleShape)
                                        .border(2.dp, theme.backgroundColor, CircleShape)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, tint = theme.backgroundColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(person.firstName, style = MaterialTheme.typography.headlineSmall, color = theme.contentColor)
                            
                            if (!isReadOnly) {
                                // Case "Décédé(e)"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isDeceased,
                                        onCheckedChange = { isDeceased = it },
                                        colors = CheckboxDefaults.colors(checkedColor = accent)
                                    )
                                    Text("Décédé(e)", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                                }
                                
                                // Bouton "Modifier identité / liens" (v9.4.24: Correction Layout)
                                OutlinedButton(
                                    onClick = onEditLinks,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor)
                                ) {
                                    Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Modifier identité / liens",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else if (person.isDeceased) {
                                Text("Décédé(e)", style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(top = 4.dp))
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
                            IconButton(onClick = { photoPicker.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = accent)
                            }
                        }
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
                                    MediaThumbnail(
                                        media = media.copy(mediaPath = resolvedUrls[media.id] ?: media.mediaPath),
                                        onRemove = { viewModel.removeMedia(media) },
                                        isReadOnly = isReadOnly
                                    )
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

@Composable
fun MediaThumbnail(media: PersonMediaEntity, onRemove: () -> Unit, isReadOnly: Boolean = false) {
    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = media.mediaPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (!isReadOnly) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
