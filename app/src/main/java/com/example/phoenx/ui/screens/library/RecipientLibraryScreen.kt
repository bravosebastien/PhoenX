package com.example.phoenx.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.screens.recipient.RecipientMediaViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipientLibraryScreen(
    navController: NavController,
    isCreatorMode: Boolean = true,
    targetCreatorId: String? = null,
    viewModel: LibraryCoverViewModel = hiltViewModel(),
    mediaViewModel: com.example.phoenx.ui.screens.recipient.RecipientMediaViewModel = hiltViewModel()
) {
    val libraryEntries by mediaViewModel.libraryEntries.collectAsState()
    val videoEntries by mediaViewModel.videoEntries.collectAsState()
    val discothequeEntries by mediaViewModel.discothequeEntries.collectAsState()
    val archiveEntries by mediaViewModel.archiveEntries.collectAsState()
    
    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(targetCreatorId) {
        mediaViewModel.setTargetCreator(targetCreatorId)
    }

    // Filtrer pour ne compter que les souvenirs racines (v8.3.4)
    val rootLibrary = libraryEntries.filter { it.parentEntryId == null }
    val rootVideo = videoEntries.filter { it.parentEntryId == null }
    val rootDisco = discothequeEntries.filter { it.parentEntryId == null }
    val rootArchive = archiveEntries.filter { it.parentEntryId == null }
    
    val totalSouvenirs = rootLibrary.size + rootVideo.size + rootDisco.size + rootArchive.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
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
                Icon(Icons.Outlined.ArrowBack, null, tint = theme.contentColor)
            }
            Text(
                text = "Ma Bibliothèque",
                style = TextStyle(
                    fontFamily = theme.fontFamily, 
                    fontStyle = FontStyle.Italic, 
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                ),
                color = theme.contentColor
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
            color = theme.contentColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp, bottom = 14.dp)
        )

        // ── 1. ESSENTIELS (Lignes fines - AGRANDIES v8.9.6) ──────────────────
        Text(
            "ESSENTIELS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
        )

        CompactEssentialRow(
            title = "Fil de Pensée",
            info = "$totalSouvenirs souvenirs classés par âge",
            icon = Icons.Outlined.Timeline,
            onClick = { 
                val route = if (isCreatorMode) "fil_pensee" else "fil_pensee?creatorId=$targetCreatorId"
                navController.navigate(route) 
            },
            theme = theme
        )

        CompactEssentialRow(
            title = "Livre de Ma Vie",
            info = if (isCreatorMode) "Co-écrit avec l'IA narrative" else "Consultation du manuscrit",
            icon = Icons.Outlined.MenuBook,
            onClick = { 
                if (isCreatorMode) {
                    navController.navigate("book_editor") 
                } else {
                    navController.navigate("book_viewer_recipient?creatorId=$targetCreatorId")
                }
            },
            theme = theme
        )

        CompactEssentialRow(
            title = "Lettre à Mon Jeune Moi",
            info = "Écris à celui que tu étais",
            icon = Icons.Outlined.HistoryEdu,
            onClick = { navController.navigate("youngselfletters") },
            theme = theme
        )

        Spacer(modifier = Modifier.height(64.dp))

        // ── 2. GRILLE DE 6 BLOCS VISIBLES (AGRANDIS v8.9.6) ──────────────────
        Text(
            "COMPARTIMENTS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 3
        ) {
            val itemModifier = Modifier.weight(1f)
            
            CompactGridItem(
                label = "Coffre Fort",
                icon = Icons.Outlined.Lock,
                onClick = { 
                    if (isCreatorMode) navController.navigate("coffre_fort")
                    else navController.navigate(Screen.RecipientDetective.createRoute(targetCreatorId))
                },
                theme = theme,
                modifier = itemModifier
            )
            CompactGridItem(
                label = "100 Questions",
                icon = Icons.Outlined.HelpOutline,
                onClick = { navController.navigate("cent_questions") },
                theme = theme,
                modifier = itemModifier
            )
            CompactGridItem(
                label = "Portraits",
                icon = Icons.Outlined.AccountCircle,
                onClick = { navController.navigate("portrait_proche") },
                theme = theme,
                modifier = itemModifier
            )
            CompactGridItem(
                label = "Mon Quiz",
                icon = Icons.Outlined.EmojiEvents,
                onClick = { if (isCreatorMode) navController.navigate("quiz_create") },
                theme = theme,
                modifier = itemModifier
            )
            CompactGridItem(
                label = "Mappemonde",
                icon = Icons.Outlined.Public,
                onClick = { navController.navigate("mappemonde") },
                theme = theme,
                modifier = itemModifier
            )
            
            // Toggle Button
            Column(
                modifier = itemModifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.MoreHoriz, 
                            null, 
                            tint = accent, 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isExpanded) "Réduire" else "Autres",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = accent,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── 3. ZONE DÉPLIÉE ───────────────────────────────
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                
                // GROUPE 1 : ACTIONS DE CRÉATION
                Text(
                    "DÉPOSER ET TRANSMETTRE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                    color = theme.contentColor.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 8.dp, top = 32.dp, bottom = 12.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3
                ) {
                    val itemModifier = Modifier.weight(1f)
                    CompactGridItem("Le Pacte", Icons.Outlined.Handshake, { navController.navigate("le_pacte") }, theme, itemModifier)
                    CompactGridItem("Réconciliation", Icons.Outlined.Mail, { navController.navigate("reconciliation") }, theme, itemModifier)
                    CompactGridItem("Capsules", Icons.Outlined.MailOutline, { navController.navigate("lettres") }, theme, itemModifier)
                    // Remplissage si nécessaire
                    repeat(2) { Spacer(modifier = itemModifier) }
                }

                // GROUPE 2 : MÉDIATHÈQUE (Auto)
                Text(
                    "MA MÉDIATHÈQUE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                    color = theme.contentColor.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 8.dp, top = 32.dp, bottom = 12.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3
                ) {
                    val itemModifier = Modifier.weight(1f)
                    CompactGridItem("Discothèque", Icons.Outlined.Album, { navController.navigate(Screen.RecipientDiscotheque.createRoute(targetCreatorId ?: mediaViewModel.currentUid)) }, theme, itemModifier)
                    CompactGridItem("Vidéothèque", Icons.Outlined.Movie, { navController.navigate(Screen.RecipientVideotheque.createRoute(targetCreatorId ?: mediaViewModel.currentUid)) }, theme, itemModifier)
                    CompactGridItem("Photos", Icons.Outlined.PhotoCamera, { navController.navigate(Screen.RecipientPhotos.createRoute(targetCreatorId ?: mediaViewModel.currentUid)) }, theme, itemModifier)
                    // Remplissage si nécessaire
                    repeat(2) { Spacer(modifier = itemModifier) }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun CompactEssentialRow(
    title: String,
    info: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    theme: AppThemeState
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = theme.accentColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = theme.accentColor, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = TextStyle(
                        fontFamily = theme.fontFamily, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 19.sp
                    ), 
                    color = theme.contentColor
                )
                Text(
                    text = info, 
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), 
                    color = theme.contentColor.copy(alpha = 0.5f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null, 
                tint = theme.contentColor.copy(alpha = 0.2f), 
                modifier = Modifier.size(22.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp), 
            color = theme.contentColor.copy(alpha = 0.1f), 
            thickness = 0.5.dp
        )
    }
}

@Composable
fun CompactGridItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = theme.contentColor.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = theme.accentColor, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = theme.fontFamily
            ),
            color = theme.contentColor.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
