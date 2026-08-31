package com.example.phoenx.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.screens.book.BookThemeOptions
import com.example.phoenx.ui.theme.*
import com.example.phoenx.ui.theme.phoenXMatiere

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewDashboardScreen(
    recipientUid: String,
    onNavigateBack: () -> Unit,
    onNavigateToFil: () -> Unit,
    onNavigateToMedia: (String) -> Unit,
    onNavigateToBook: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToGenealogy: () -> Unit,
    onNavigateToPersonalities: () -> Unit, // v9.7.0
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // APPLICATION DE L'AMBIANCE (Action Chantier C)
    val customFont = remember(state.ambiance.fontId) { BookThemeOptions.getFont(state.ambiance.fontId) }
    val customBg = remember(state.ambiance.backgroundId) { BookThemeOptions.getBackground(state.ambiance.backgroundId) }
    
    val theme = LocalAppTheme.current.copy(
        fontFamily = customFont,
        backgroundColor = customBg.color,
        contentColor = if (customBg.darkText) Color.Black else Color.White
    )
    val accent = theme.accentColor

    LaunchedEffect(recipientUid) {
        viewModel.loadPreview(recipientUid)
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
        Scaffold(
            containerColor = theme.backgroundColor,
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Aperçu Vision Destinataire", style = MaterialTheme.typography.labelSmall, color = accent)
                            Text(state.recipientName, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // BANDEAU PRÉVENTION
                    Surface(
                        color = accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = accent)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Ce que vous voyez ici reflète fidèlement le contenu et l'ambiance de l'espace de ${state.recipientName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.contentColor.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Text(
                        "ESPACE DE TRANSMISSION", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                        color = theme.contentColor.copy(alpha = 0.4f)
                    )

                    // LES 4 PILIERS DE L'APERÇU
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PreviewPillarItem(
                            title = "Souvenirs & Récits",
                            count = state.souvenirsCount,
                            icon = Icons.Default.HistoryEdu,
                            accent = accent,
                            onClick = onNavigateToFil
                        )
                        PreviewPillarItem(
                            title = "Photos",
                            count = state.photosCount,
                            icon = Icons.Default.PhotoLibrary,
                            accent = accent,
                            onClick = { onNavigateToMedia("PHOTO") }
                        )
                        PreviewPillarItem(
                            title = "Vidéos",
                            count = state.videosCount,
                            icon = Icons.Default.VideoLibrary,
                            accent = accent,
                            onClick = { onNavigateToMedia("VIDEO") }
                        )
                        PreviewPillarItem(
                            title = "Audios",
                            count = state.audiosCount,
                            icon = Icons.Default.MusicNote,
                            accent = accent,
                            onClick = { onNavigateToMedia("AUDIO") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        PreviewPillarItem(
                            title = "Livre de Vie",
                            count = if (state.hasBookDraft) 1 else 0,
                            icon = Icons.Default.AutoStories,
                            accent = accent,
                            onClick = onNavigateToBook
                        )

                        PreviewPillarItem(
                            title = "Coffre-Fort",
                            count = state.filteredEnigmas.size,
                            icon = Icons.Default.Lock,
                            accent = accent,
                            onClick = onNavigateToVault
                        )

                        PreviewPillarItem(
                            title = "Arbre Généalogique",
                            count = state.familyCount,
                            icon = Icons.Default.Public,
                            accent = accent,
                            onClick = onNavigateToGenealogy
                        )

                        PreviewPillarItem(
                            title = "Personnalités",
                            count = 0, // À brancher sur le vrai count si besoin
                            icon = Icons.Default.Star,
                            accent = accent,
                            onClick = onNavigateToPersonalities
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        "Note : Cet aperçu ne permet aucune modification. C'est une vue en lecture seule pour votre sérénité.",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.4f),
                        fontStyle = FontStyle.Italic,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewPillarItem(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                val label = if (count <= 1) "élément partagé" else "éléments partagés"
                Text("$count $label", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.5f))
            }
            Icon(Icons.Default.ChevronRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
        }
    }
}
