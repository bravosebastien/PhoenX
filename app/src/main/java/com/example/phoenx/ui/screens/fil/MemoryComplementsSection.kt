package com.example.phoenx.ui.screens.fil

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
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.AppThemeState
import com.example.phoenx.ui.theme.Error

/**
 * MemoryComplementsSection — Gestion de la galerie de médias rattachés (Photos, Vidéos, Audios).
 * v8.9.8 : Extraction modulaire.
 */
@Composable
fun MemoryComplementsSection(
    entryId: String,
    complements: List<OfflineEntry>,
    targetCreatorId: String?,
    viewModel: MemoryDetailViewModel,
    theme: AppThemeState,
    accent: Color,
    navController: NavController
) {
    var showAddMediaMenu by remember { mutableStateOf(false) }

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
                    val types = listOf(
                        Triple("Texte", Icons.Default.Description, "TEXT"),
                        Triple("Photo", Icons.Default.PhotoCamera, "PHOTO"),
                        Triple("Galerie", Icons.Default.Collections, "GALLERY"),
                        Triple("Vocal", Icons.Default.Mic, "AUDIO")
                    )
                    types.forEach { (label, icon, type) ->
                        DropdownMenuItem(
                            text = { Text(if (type == "TEXT") "Ajouter un récit" else label, color = theme.contentColor) },
                            leadingIcon = { Icon(icon, null, tint = accent) },
                            onClick = {
                                showAddMediaMenu = false
                                navController.navigate(Screen.Capture.createRoute(type = type, parentEntryId = entryId))
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
                            if (complement.entryType == "PHOTO" || complement.entryType == "GALLERY") {
                                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                                    AsyncImage(
                                        model = complement.localMediaPath ?: complement.mediaUrl,
                                        contentDescription = null,
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
