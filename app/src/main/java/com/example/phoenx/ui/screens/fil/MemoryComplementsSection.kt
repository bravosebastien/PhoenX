package com.example.phoenx.ui.screens.fil

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.AppThemeState
import com.example.phoenx.ui.theme.Error
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import android.net.Uri

/**
 * MemoryComplementsSection — Gestion de la galerie de médias rattachés (Photos, Vidéos, Audios).
 * v9.4.26 : Unification et sélection multiple.
 * v9.4.27 : Édition Titre & Couverture pour Notes Vocales.
 */
@Composable
fun MemoryComplementsSection(
    entryId: String,
    complements: List<OfflineEntry>,
    targetCreatorId: String?,
    viewModel: MemoryDetailViewModel,
    theme: AppThemeState,
    accent: Color,
    navController: NavController,
    isReadOnly: Boolean = false,
    onStartAudioRecording: () -> Unit = {} // v9.4.27
) {
    var showAddMediaMenu by remember { mutableStateOf(false) }
    var editingAudioId by remember { mutableStateOf<String?>(null) } // v9.4.27
    var initialAudioTitle by remember { mutableStateOf("") }

    val context = LocalContext.current
    val heirKey by viewModel.heirKey.collectAsState()

    // SÉLECTEUR DE COUVERTURE (v9.4.27)
    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && editingAudioId != null) {
            viewModel.updateComplementCover(editingAudioId!!, uri)
            editingAudioId = null
        }
    }

    // SÉLECTEUR MULTIPLE (v9.4.26)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val file = viewModel.uriToFile(uri)
                if (file != null) {
                    val mime = context.contentResolver.getType(uri)
                    val type = if (mime?.contains("video") == true) "VIDEO" else "PHOTO"
                    viewModel.addMediaComplement(entryId, file, type)
                }
            }
        }
    }

    // Récupération du MediaManager via EntryPoint
    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "COMPLÉMENTS MÉDIA", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                color = theme.contentColor.copy(alpha = 0.4f), 
                letterSpacing = 2.sp
            )
            if (!isReadOnly) {
                Box {
                    IconButton(
                        onClick = { showAddMediaMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, null, tint = accent)
                    }

                    DropdownMenu(
                        expanded = showAddMediaMenu,
                        onDismissRequest = { showAddMediaMenu = false },
                        containerColor = theme.backgroundColor
                    ) {
                        DropdownMenuItem(
                            text = { Text("Photos / Vidéos", color = theme.contentColor) },
                            leadingIcon = { Icon(Icons.Default.Collections, null, tint = accent) },
                            onClick = {
                                showAddMediaMenu = false
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Note Vocale", color = theme.contentColor) },
                            leadingIcon = { Icon(Icons.Default.Mic, null, tint = accent) },
                            onClick = {
                                showAddMediaMenu = false
                                onStartAudioRecording() // ACTION DIRECTE AVEC CHECK PERMISSIONS (v9.4.27)
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (complements.isEmpty()) {
            Text(
                "Aucun média complémentaire rattaché.", 
                style = MaterialTheme.typography.bodySmall, 
                color = theme.contentColor.copy(alpha = 0.4f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                complements.filter { it.entryType != "TEXT" }.forEach { complement ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(
                                    Screen.MediaViewer.createRoute(complement.id, targetCreatorId)
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (complement.entryType == "PHOTO" || complement.entryType == "GALLERY" || 
                                (complement.entryType == "AUDIO" && complement.coverUrl != null)) {
                                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                                    SecureAsyncImage(
                                        mediaUrl = complement.coverUrl ?: complement.mediaUrl, // v9.4.27 : Priorité couverture
                                        localPath = complement.localCoverPath ?: complement.localMediaPath,
                                        explicitKey = heirKey,
                                        mediaManager = mediaManager,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.size(60.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = accent.copy(alpha = 0.1f)
                                ) {
                                    val icon = when(complement.entryType) {
                                        "VIDEO" -> Icons.Default.Videocam
                                        "AUDIO" -> Icons.Default.Mic
                                        else -> Icons.Default.Description
                                    }
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = complement.aiSummary.ifEmpty { "Média ${complement.entryType.lowercase()}" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = theme.contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // ... (visibilité et sync)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (complement.visibility == "EVERYONE") Icons.Default.Public else Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = theme.contentColor.copy(alpha = 0.4f)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (complement.visibility == "EVERYONE") "Public" else "Restreint",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.contentColor.copy(alpha = 0.4f)
                                    )
                                    
                                    if (complement.syncStatus == "pending") {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(10.dp), tint = accent.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            if (!isReadOnly) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (complement.entryType == "AUDIO") {
                                        IconButton(onClick = { 
                                            editingAudioId = complement.id
                                            initialAudioTitle = complement.aiSummary
                                        }) {
                                            Icon(Icons.Default.Edit, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteComplement(complement.id) }) {
                                        Icon(Icons.Default.DeleteOutline, null, tint = Error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGUE ÉDITION AUDIO (v9.4.27)
    if (editingAudioId != null && !isReadOnly) {
        var editedTitle by remember { mutableStateOf(initialAudioTitle) }
        AlertDialog(
            onDismissRequest = { editingAudioId = null },
            containerColor = theme.backgroundColor,
            title = { Text("Personnaliser la Note Vocale", color = theme.contentColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Titre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(
                        onClick = { coverLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.1f), contentColor = accent)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Changer la pochette")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.updateComplementTitle(editingAudioId!!, editedTitle)
                    editingAudioId = null
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { editingAudioId = null }) { Text("Annuler") }
            }
        )
    }
}
