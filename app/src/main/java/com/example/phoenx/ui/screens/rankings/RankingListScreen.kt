package com.example.phoenx.ui.screens.rankings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.domain.model.CompartmentIds
import com.example.phoenx.domain.model.Ranking
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.screens.library.LibraryCoverViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors
import com.example.phoenx.data.media.MediaManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RankingListScreen(
    navController: NavController,
    targetCreatorId: String? = null,
    viewModel: RankingViewModel = hiltViewModel(),
    coverViewModel: LibraryCoverViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val rankings by viewModel.allRankings.collectAsState()
    val covers by coverViewModel.covers.collectAsState()
    val context = LocalContext.current
    
    val isReadOnly = targetCreatorId != null

    LaunchedEffect(targetCreatorId) {
        viewModel.setTargetCreator(targetCreatorId)
    }

    val mediaManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaManager.MediaManagerEntryPoint::class.java
        ).mediaManager()
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Mes Classements", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    InfoButton(
                        title = "Mes Classements",
                        points = listOf(
                            "Créez vos tops personnels par thématique.",
                            "C'est un espace de pur plaisir pour partager vos goûts et vos coups de cœur.",
                            "Choisissez une catégorie ou créez la vôtre, puis remplissez vos rangs préférés."
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = accent,
                    contentColor = theme.backgroundColor,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. VISUEL DE TIROIR (Identique aux autres compartiments)
            item {
                val rankingCover = covers[CompartmentIds.RANKINGS]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.contentColor.copy(alpha = 0.05f))
                        .clickable(enabled = !isReadOnly) {
                            navController.navigate("library_cover_picker/${CompartmentIds.RANKINGS}/Mes Classements")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (rankingCover != null) {
                        SecureAsyncImage(
                            mediaUrl = rankingCover.mediaUrl,
                            mediaManager = mediaManager,
                            isEncrypted = false,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            creatorId = targetCreatorId,
                            docType = "libraryCover",
                            docId = CompartmentIds.RANKINGS
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, null, tint = theme.contentColor.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(if (isReadOnly) "Pas d'image d'ambiance" else "Ajouter une image d'ambiance", color = theme.contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 2. IDÉES POUR COMMENCER (Puces)
            if (!isReadOnly) {
                item {
                    RankingSuggestionsSection(
                        onSelect = { title -> viewModel.createRanking(title, 5) },
                        accent = accent,
                        theme = theme
                    )
                }
            }

            // 3. LISTE DES CLASSEMENTS
            if (rankings.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun classement pour le moment.", color = theme.contentColor.copy(alpha = 0.4f))
                    }
                }
            } else {
                items(rankings) { ranking ->
                    RankingItem(
                        ranking = ranking,
                        onClick = { navController.navigate(Screen.RankingDetail.createRoute(ranking.id, targetCreatorId)) },
                        theme = theme,
                        accent = accent,
                        mediaManager = mediaManager,
                        targetCreatorId = targetCreatorId
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRankingDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, count ->
                viewModel.createRanking(title, count)
                showCreateDialog = false
            },
            accent = accent,
            theme = theme
        )
    }
}

@Composable
fun RankingItem(
    ranking: Ranking,
    onClick: () -> Unit,
    theme: com.example.phoenx.ui.theme.AppThemeState,
    accent: Color,
    mediaManager: MediaManager,
    targetCreatorId: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vignette Image ou Invite
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.contentColor.copy(alpha = 0.05f))
                    .then(
                        if (ranking.coverImageUrl == null) 
                            Modifier.border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (ranking.coverImageUrl != null) {
                    SecureAsyncImage(
                        mediaUrl = ranking.coverImageUrl,
                        mediaManager = mediaManager,
                        isEncrypted = false,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        creatorId = targetCreatorId,
                        docType = "rankings",
                        docId = ranking.id,
                        field = "coverImageUrl"
                    )
                } else {
                    Text(
                        "Insérez une image",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        textAlign = TextAlign.Center,
                        color = accent.copy(alpha = 0.6f),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ranking.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val filledCount = ranking.items.count { it.isNotBlank() }
                Text(
                    text = "$filledCount / ${ranking.itemCount} éléments remplis",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.5f)
                )
            }

            Icon(Icons.Default.ChevronRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun RankingSuggestionsSection(
    onSelect: (String) -> Unit,
    accent: Color,
    theme: com.example.phoenx.ui.theme.AppThemeState
) {
    val suggestions = mapOf(
        "Musique" to listOf("Musiciens préférés", "Chanteuses préférées", "Albums préférés", "Chansons préférées", "Plus belles paroles", "Plus beaux solos de guitare", "Concerts inoubliables"),
        "Cinéma et séries" to listOf("Films préférés", "Acteurs préférés", "Actrices préférées", "Réalisateurs préférés", "Scènes cultes", "Répliques cultes", "Séries préférées"),
        "Littérature" to listOf("Livres préférés", "Auteurs préférés", "Citations préférées", "Personnages de roman marquants"),
        "Art visuel" to listOf("Peintures préférées", "Peintres préférés", "Sculptures préférées", "Photographies préférées"),
        "Lieux et voyages" to listOf("Plus beaux voyages", "Plus beaux paysages", "Villes préférées", "Souvenirs de vacances"),
        "Vie personnelle" to listOf("Souvenirs de vie", "Personnes qui ont le plus compté", "Plus grandes fiertés", "Plats préférés", "Recettes de famille", "Moments en famille"),
        "Sport" to listOf("Sportifs préférés", "Matchs inoubliables", "Moments sportifs préférés")
    )

    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            "IDÉES POUR COMMENCER",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
        )

        suggestions.forEach { (category, items) ->
            var isExpanded by remember { mutableStateOf(false) }
            
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    color = Color.Transparent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = accent.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = accent.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                AnimatedVisibility(visible = isExpanded) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEach { item ->
                            SuggestionChip(
                                label = { Text(item, fontSize = 12.sp) },
                                onClick = { onSelect(item) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = theme.contentColor.copy(alpha = 0.8f)
                                ),
                                border = BorderStroke(0.5.dp, theme.contentColor.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = theme.contentColor.copy(alpha = 0.05f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
fun CreateRankingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
    accent: Color,
    theme: com.example.phoenx.ui.theme.AppThemeState
) {
    var title by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text("Nouveau classement", color = theme.contentColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.replace("|", "") },
                    label = { Text("Thématique") },
                    placeholder = { Text("Ex: Mes films cultes") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
                
                Column {
                    Text("Nombre d'éléments : $count", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor)
                    Slider(
                        value = count.toFloat(),
                        onValueChange = { count = it.toInt() },
                        valueRange = 3f..20f,
                        steps = 16,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, count) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Créer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    )
}
