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
    
    // Simulation des stats (à lier au VM si besoin)
    val totalSouvenirs = 42 

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
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
                Icon(Icons.Outlined.ArrowBack, null, tint = AccentPrimary)
            }
            Text(
                text = "Ma Bibliothèque",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 20.sp),
                color = TextPrimary
            )
            Row {
                Icon(Icons.Outlined.Info, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Outlined.Search, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
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
            color = AccentPrimary,
            modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
        )

        EssentialCard(
            title = "Fil de Pensée",
            description = "Souvenirs classés par l'âge que tu avais.",
            status = "$totalSouvenirs souvenirs",
            icon = Icons.Outlined.Timeline,
            onClick = { navController.navigate("fil_pensee") },
            onEdit = { navController.navigate("library_cover_picker/fil_pensee/Fil de Pensée") }
        )

        EssentialCard(
            title = "Livre de Ma Vie",
            description = "Co-écrit avec l'IA narrative.",
            status = "En cours",
            icon = Icons.Outlined.MenuBook,
            onClick = { navController.navigate("book_editor") },
            onEdit = { navController.navigate("library_cover_picker/livre_vie/Livre de Ma Vie") }
        )

        EssentialCard(
            title = "Lettre à Mon Jeune Moi",
            description = "Écris à celui que tu étais.",
            status = "1 lettre active",
            icon = Icons.Outlined.HistoryEdu,
            onClick = { navController.navigate("youngselfletters") },
            onEdit = { navController.navigate("library_cover_picker/jeune_moi/Lettre à Mon Jeune Moi") }
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
            Triple("Discothèque", Icons.Outlined.Album, "library_music"),
            Triple("Vidéothèque", Icons.Outlined.Movie, "library_video"),
            Triple("Mes Meilleurs", Icons.Outlined.StarOutline, "mes_meilleurs"),
            Triple("Photos", Icons.Outlined.PhotoCamera, "photos"),
            Triple("Mappemonde", Icons.Outlined.Public, "mappemonde"),
            Triple("100 Questions", Icons.Outlined.HelpOutline, "cent_questions"),
            Triple("Coffre Fort", Icons.Outlined.Lock, "coffre_fort"),
            Triple("Le Pacte", Icons.Outlined.Handshake, "le_pacte"),
            Triple("Portrait proche", Icons.Outlined.AccountCircle, "portrait_proche"),
            Triple("Réconciliation", Icons.Outlined.Mail, "reconciliation"),
            Triple("Lettres", Icons.Outlined.MailOutline, "lettres"),
            Triple("Tiroir secret", Icons.Outlined.Key, "tiroir_secret")
        )

        // Affichage en grille manuelle pour éviter le LazyVerticalGrid dans Scrollable
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            compartments.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    rowItems.forEach { comp ->
                        CompartmentCard(
                            name = comp.first,
                            icon = comp.second,
                            cover = covers[comp.first.lowercase().replace(" ", "_")],
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(comp.third) },
                            onEdit = { navController.navigate("library_cover_picker/${comp.first.lowercase().replace(" ", "_")}/${comp.first}") }
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
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211E1A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.33f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Halos radiaux (Simulation simplifiée)
            Box(modifier = Modifier.align(Alignment.TopEnd).size(100.dp).background(Brush.radialGradient(listOf(AccentPrimary.copy(alpha = 0.15f), Color.Transparent))))
            
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = AccentPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = AccentPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 16.sp), color = TextPrimary)
                    Text(description, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp), color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(4.dp).background(AccentPrimary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(status.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AccentPrimary)
                    }
                }
                
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(22.dp).background(AccentPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).border(1.dp, AccentPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = AccentPrimary, modifier = Modifier.size(12.dp))
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
    Card(
        modifier = modifier.height(95.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E23)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.25f))
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
            
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(AccentPrimary.copy(alpha = 0.4f)))
            
            IconButton(
                onClick = onEdit,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp)
            ) {
                Icon(Icons.Outlined.Edit, null, tint = AccentPrimary.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
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
