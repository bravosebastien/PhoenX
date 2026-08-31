package com.example.phoenx.ui.screens.personalities

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.local.PersonalityEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAppTheme
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalitiesScreen(
    navController: NavController,
    targetCreatorId: String? = null,
    heirKey: ByteArray? = null,
    viewModel: PersonalitiesViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val personalities by viewModel.filteredPersonalities.collectAsState()
    val activeFilter by viewModel.categoryFilter.collectAsState()
    val context = LocalContext.current

    val categories = listOf("Sport", "Cinéma", "Peinture", "Sculpture", "Sciences", "Symboles de la paix", "Symboles du chaos", "Symboles de l'amour", "Symboles de la haine", "Autre")

    val isReadOnly = targetCreatorId != null

    LaunchedEffect(targetCreatorId, heirKey) {
        viewModel.setTargetCreator(targetCreatorId, heirKey)
    }

    val mediaManager = remember {
        EntryPointAccessors.fromApplication(context, MediaManager.MediaManagerEntryPoint::class.java).mediaManager()
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            Column(modifier = Modifier.background(theme.backgroundColor)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Personnalités",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = theme.contentColor
                            )
                            Text(
                                "Figures inspirantes ou marquantes",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.contentColor.copy(alpha = 0.5f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        InfoButton(
                            title = "Personnalités",
                            points = listOf(
                                "Cet espace est dédié aux figures publiques (artistes, sportifs, scientifiques, chefs d'état...) qui ont compté dans votre vie.",
                                "Contrairement aux Souvenirs, ces fiches sont visibles par TOUS vos Destinataires sans exception une fois votre héritage activé.",
                                "Vous pouvez y partager votre biographie préférée de ces personnes et surtout expliquer en quoi elles vous ont influencé."
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                // Filtres de catégorie
                ScrollableTabRow(
                    selectedTabIndex = if (activeFilter == null) 0 else categories.indexOf(activeFilter) + 1,
                    containerColor = Color.Transparent,
                    edgePadding = 20.dp,
                    divider = {},
                    indicator = {}
                ) {
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { viewModel.updateCategoryFilter(null) },
                        label = { Text("Tous") },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = activeFilter == category,
                            onClick = { viewModel.updateCategoryFilter(category) },
                            label = { Text(category) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = { navController.navigate("personality_edit/new") },
                    containerColor = accent,
                    contentColor = theme.backgroundColor,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->
        if (personalities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Aucune personnalité ajoutée.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(personalities) { personality ->
                    PersonalityCard(
                        personality = personality,
                        mediaManager = mediaManager,
                        targetCreatorId = targetCreatorId,
                        heirKey = heirKey,
                        onClick = {
                            navController.navigate("personality_detail/${personality.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalityCard(
    personality: PersonalityEntity,
    mediaManager: MediaManager,
    targetCreatorId: String?,
    heirKey: ByteArray?,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.contentColor.copy(alpha = 0.05f))
        ) {
            SecureAsyncImage(
                mediaUrl = personality.mainPhotoPath,
                mediaManager = mediaManager,
                creatorId = targetCreatorId,
                explicitKey = heirKey,
                docType = "personalities",
                docId = personality.id,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = personality.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = if (personality.category == "Autre") personality.customCategoryLabel ?: "Autre" else personality.category,
            style = MaterialTheme.typography.labelSmall,
            color = theme.accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
