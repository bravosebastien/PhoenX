package com.example.phoenx.ui.screens.personalities

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.local.PersonalityEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalityDetailScreen(
    personalityId: String,
    navController: NavController,
    targetCreatorId: String? = null,
    heirKey: ByteArray? = null,
    viewModel: PersonalitiesViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val personality by viewModel.personalities.collectAsState()
    val item = personality.find { it.id == personalityId }
    
    // v9.7.2 : Correction Bug 2 - Utilisation de remember pour éviter la recréation du Flow
    val mediaList by remember(personalityId) { 
        viewModel.getMediaForPersonality(personalityId) 
    }.collectAsState(initial = emptyList())

    val context = LocalContext.current

    android.util.Log.d("PHOENX_PERSO_UI", "Détail Personnalité ouvert. ID reçu: $personalityId")

    var readingMode by remember { mutableStateOf<String?>(null) } // "BIO" | "COMMENT"
    var showDeleteConfirm by remember { mutableStateOf(false) } // v9.7.5

    val isReadOnly = targetCreatorId != null

    val mediaManager = remember {
        EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager()
    }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }
        return
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(theme.backgroundColor)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    SecureAsyncImage(
                        mediaUrl = item.mainPhotoPath,
                        mediaManager = mediaManager,
                        creatorId = targetCreatorId,
                        explicitKey = heirKey,
                        docType = "personalities",
                        docId = item.id,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        isEncrypted = false // v9.7.4 : Photo publique non chiffrée
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, theme.backgroundColor.copy(alpha = 0.8f)),
                                    startY = 200f
                                )
                            )
                    )

                    // 1. Bouton RETOUR (Haut-Gauche)
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(12.dp)
                            .size(40.dp) // Zone de clic
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp) // Halo réduit v9.7.9
                                .background(theme.contentColor.copy(alpha = 0.1f), CircleShape)
                                .border(0.5.dp, accent.copy(alpha = 0.3f), CircleShape)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = null, 
                            tint = theme.contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!isReadOnly) {
                        // 2. Bouton ÉDITER (Haut-Droite)
                        Box(
                            modifier = Modifier
                                .statusBarsPadding()
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(40.dp)
                                .clickable { navController.navigate("personality_edit/${item.id}") },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(theme.contentColor.copy(alpha = 0.1f), CircleShape)
                                    .border(0.5.dp, accent.copy(alpha = 0.3f), CircleShape)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit, 
                                contentDescription = "Modifier", 
                                tint = theme.contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // 3. Bouton SUPPRIMER (Bas-Droite)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(40.dp)
                                .clickable { showDeleteConfirm = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(theme.contentColor.copy(alpha = 0.1f), CircleShape)
                                    .border(0.5.dp, accent.copy(alpha = 0.3f), CircleShape)
                            )
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = "Supprimer", 
                                tint = theme.contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = theme.fontFamily, // v9.7.6 : Style plume
                            fontWeight = FontWeight.Bold
                        ),
                        color = theme.contentColor
                    )
                    
                    Surface(
                        color = theme.contentColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 8.dp).alpha(0.7f)
                    ) {
                        Text(
                            text = if (item.category == "Autre") item.customCategoryLabel ?: "Autre" else item.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = theme.contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Biographie (Cliquable)
                    SectionHeader(title = "BIOGRAPHIE")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable { readingMode = "BIO" },
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.02f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, theme.contentColor.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = item.biography.ifBlank { "Aucune biographie renseignée." },
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                            color = theme.contentColor.copy(alpha = 0.8f),
                            modifier = Modifier.padding(16.dp),
                            maxLines = 6,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Commentaire Personnel (Cliquable)
                    SectionHeader(title = "POURQUOI CETTE PERSONNE ?")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable { readingMode = "COMMENT" },
                        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.03f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, accent.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = item.personalComment.ifBlank { "Aucun commentaire personnel." },
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, fontStyle = FontStyle.Italic),
                            color = theme.contentColor.copy(alpha = 0.9f),
                            modifier = Modifier.padding(16.dp),
                            maxLines = 6,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Petite Galerie (v9.7.2 : Logs de diagnostic et rendu forcé)
                    android.util.Log.d("PHOENX_PERSO_UI", "Rendu Galerie: ${mediaList.size} items trouvés pour $personalityId")
                    
                    if (mediaList.isNotEmpty()) {
                        Spacer(Modifier.height(32.dp))
                        SectionHeader(title = "GALERIE PHOTOS")
                        Spacer(Modifier.height(16.dp))
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp), // Hauteur fixe pour garantir la visibilité
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(
                                items = mediaList,
                                key = { it.id } // Clé unique pour aider Compose
                            ) { media ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(theme.contentColor.copy(alpha = 0.05f))
                                        .clickable {
                                            android.util.Log.d("PHOENX_PERSO_UI", "Clic photo: ${media.id}")
                                            navController.navigate(
                                                com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(
                                                    entryId = media.id,
                                                    creatorId = targetCreatorId,
                                                    mediaUrl = media.mediaPath,
                                                    entryType = "PHOTO",
                                                    aiSummary = "Photo de ${item.name}",
                                                    sourceDocType = "personalityMedia",
                                                    personId = personalityId, // Correction: on passe l'ID de la personnalité parente
                                                    isEncrypted = false
                                                )
                                            )
                                        }
                                ) {
                                    SecureAsyncImage(
                                        mediaUrl = media.mediaPath,
                                        mediaManager = mediaManager,
                                        creatorId = targetCreatorId,
                                        explicitKey = heirKey,
                                        docType = "personalityMedia",
                                        docId = media.id,
                                        personId = personalityId, // Correction
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        isEncrypted = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }

            // FULL SCREEN READING VIEWS
            AnimatedVisibility(
                visible = readingMode != null,
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
            ) {
                val title = if (readingMode == "BIO") "BIOGRAPHIE" else "COMMENTAIRE"
                val content = if (readingMode == "BIO") item.biography else item.personalComment
                val isItalic = readingMode == "COMMENT"

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = theme.backgroundColor
                ) {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                title, 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                                color = accent
                            )
                            IconButton(onClick = { readingMode = null }) {
                                Icon(Icons.Default.Close, null, tint = theme.contentColor)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                                color = theme.contentColor
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 32.sp,
                                    fontSize = 18.sp,
                                    fontStyle = if (isItalic) FontStyle.Italic else null
                                ),
                                color = theme.contentColor.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = theme.backgroundColor,
            title = { Text("Supprimer ${item.name} ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Cette action est irréversible. Toutes les informations et photos associées seront définitivement supprimées.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePersonality(item)
                        showDeleteConfirm = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.phoenx.ui.theme.Error)
                ) {
                    Text("Supprimer définitivement", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black, 
            letterSpacing = 1.5.sp
        ),
        color = accent.copy(alpha = 0.4f) // v9.7.6 : Utilise une nuance de l'accent
    )
}
