package com.example.phoenx.ui.screens.personalities

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                        contentScale = ContentScale.Crop
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

                    // Back Button
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }

                    // Edit Button (Creator Only)
                    if (!isReadOnly) {
                        IconButton(
                            onClick = { navController.navigate("personality_edit/${item.id}") },
                            modifier = Modifier
                                .statusBarsPadding()
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(accent, CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = theme.backgroundColor)
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    )
                    
                    Surface(
                        color = accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = if (item.category == "Autre") item.customCategoryLabel ?: "Autre" else item.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
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
                        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.1f))
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
}

@Composable
private fun SectionHeader(title: String) {
    val theme = LocalAppTheme.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = theme.contentColor.copy(alpha = 0.4f)
    )
}
