package com.example.phoenx.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun RecipientLibraryScreen(
    navController: NavController,
    isCreatorMode: Boolean = true,
    viewModel: LibraryCoverViewModel = hiltViewModel()
) {
    val covers by viewModel.covers.collectAsState()
    val accent = LocalAccentColor.current
    
    // Simulation des stats (à lier au VM si besoin)
    val totalSouvenirs = 42 

    android.util.Log.d("LibraryCover", "Covers chargées : ${covers.keys}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalBackgroundBrush.current)
            .verticalScroll(rememberScrollState())
    ) {
        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Outlined.ArrowBack, null, tint = accent)
            }
            Text(
                text = "Ma Bibliothèque",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 20.sp),
                color = TextPrimary
            )
            Row {
                Icon(Icons.Outlined.Info, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Outlined.Search, null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }

        Text(
            text = "15 compartiments · $totalSouvenirs souvenirs déposés",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextTertiary,
            modifier = Modifier.padding(start = 16.dp, bottom = 14.dp)
        )

        // ESSENTIELS
        Text(
            "ESSENTIELS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = accent,
            modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
        )

        EssentialCard(
            title = "Fil de Pensée",
            description = "Souvenirs classés par l'âge que tu avais.",
            status = "$totalSouvenirs souvenirs",
            icon = Icons.Outlined.Timeline,
            cover = covers["fil_pensee"],
            onClick = { navController.navigate("fil_pensee") },
            onEdit = { navController.navigate("library_cover_picker/fil_pensee/Fil de Pensée") }
        )

        EssentialCard(
            title = "Livre de Ma Vie",
            description = "Co-écrit avec l'IA narrative.",
            status = "En cours",
            icon = Icons.Outlined.MenuBook,
            cover = covers["livre_vie"],
            onClick = { navController.navigate("book_editor") },
            onEdit = { navController.navigate("library_cover_picker/livre_vie/Livre de Ma Vie") }
        )

        EssentialCard(
            title = "Lettre à Mon Jeune Moi",
            description = "Écris à celui que tu étais.",
            status = "1 lettre active",
            icon = Icons.Outlined.HistoryEdu,
            cover = covers["lettre_jeune_moi"],
            onClick = { navController.navigate("youngselfletters") },
            onEdit = { navController.navigate("library_cover_picker/lettre_jeune_moi/Lettre à Mon Jeune Moi") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TOUS LES COMPARTIMENTS
        Text(
            "TOUS LES COMPARTIMENTS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
            color = TextTertiary,
            modifier = Modifier.padding(start = 14.dp, bottom = 6.dp)
        )

        val compartments = listOf(
            // Triple("Label", Icon, "route", "ID pour cover")
            listOf("Discothèque", Icons.Outlined.Album, "library_music", "discotheque"),
            listOf("Vidéothèque", Icons.Outlined.Movie, "library_video", "videotheque"),
            listOf("Mes Meilleurs", Icons.Outlined.StarOutline, "mes_meilleurs", "mes_meilleurs"),
            listOf("Photos", Icons.Outlined.PhotoCamera, "photos", "photos"),
            listOf("Mappemonde", Icons.Outlined.Public, "mappemonde", "mappemonde"),
            listOf("100 Questions", Icons.Outlined.HelpOutline, "cent_questions", "cent_questions"),
            listOf("Coffre Fort", Icons.Outlined.Lock, "coffre_fort", "coffre_fort"),
            listOf("Le Pacte", Icons.Outlined.Handshake, "le_pacte", "le_pacte"),
            listOf("Portrait proche", Icons.Outlined.AccountCircle, "portrait_proche", "portrait_proche"),
            listOf("Réconciliation", Icons.Outlined.Mail, "reconciliation", "reconciliation"),
            listOf("Lettres", Icons.Outlined.MailOutline, "lettres", "lettres"),
            listOf("Tiroir secret", Icons.Outlined.Key, "tiroir_secret", "tiroir_secret"),
            listOf("Mon Quiz", Icons.Outlined.EmojiEvents, "quiz", "quiz")
        )

        // Affichage en grille manuelle pour éviter le LazyVerticalGrid dans Scrollable
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            compartments.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    rowItems.forEach { comp ->
                        val label = comp[0] as String
                        val icon = comp[1] as androidx.compose.ui.graphics.vector.ImageVector
                        val route = comp[2] as String
                        val id = comp[3] as String
                        
                        CompartmentCard(
                            name = label,
                            icon = icon,
                            cover = covers[id],
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (label == "Mon Quiz") {
                                    if (isCreatorMode) {
                                        navController.navigate("quiz_create")
                                    } else {
                                        // Côté destinataire
                                    }
                                } else {
                                    navController.navigate(route)
                                }
                            },
                            onEdit = { 
                                android.util.Log.d("LibraryCover", "ID cherché : $id")
                                navController.navigate("library_cover_picker/$id/$label") 
                            }
                        )
                    }
                    // Compléter la ligne si moins de 3 items
                    if (rowItems.size < 3) {
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun EssentialCard(
    title: String,
    description: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cover: LibraryCover? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val accent = LocalAccentColor.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211E1A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.33f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            if (cover != null) {
                AsyncImage(
                    model = cover.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            }
            
            // Halo coin supérieur droit
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.15f), Color.Transparent),
                            radius = 120f
                        )
                    )
            )
            
            // Halo dégradé en bas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, accent.copy(alpha = 0.08f)),
                            start = Offset(0f, 0f),
                            end = Offset(0f, Float.POSITIVE_INFINITY)
                        )
                    )
            )
            
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (cover != null) Color.White.copy(alpha = 0.1f) else accent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 16.sp), color = TextPrimary)
                    Text(description, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp), color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(4.dp).background(accent, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(status.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = accent)
                    }
                }
                
                IconButton(
                    onClick = {
                        android.util.Log.d("LibraryCover", "Edit Essential: $title")
                        onEdit()
                    },
                    modifier = Modifier.size(22.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = accent, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun CompartmentCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cover: LibraryCover?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val accent = LocalAccentColor.current
    
    Card(
        modifier = modifier.height(95.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cover != null) {
                AsyncImage(
                    model = cover.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            }
            
            // Liseré top
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(accent.copy(alpha = 0.4f)))
            
            // Halo bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, accent.copy(alpha = 0.05f))
                        )
                    )
            )

            IconButton(
                onClick = onEdit,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp)
            ) {
                Icon(Icons.Outlined.Edit, null, tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    name, 
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 10.sp), 
                    color = TextSecondary, 
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
