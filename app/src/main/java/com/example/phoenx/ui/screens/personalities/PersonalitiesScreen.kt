package com.example.phoenx.ui.screens.personalities

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

    val categories = PersonalityEntity.CATEGORIES

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

                // Filtres de catégorie (Style épuré v9.7.6)
                ScrollableTabRow(
                    selectedTabIndex = if (activeFilter == null) 0 else categories.indexOf(activeFilter) + 1,
                    containerColor = Color.Transparent,
                    edgePadding = 20.dp,
                    divider = {},
                    indicator = {},
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val tabModifier = Modifier.padding(horizontal = 4.dp)
                    
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { viewModel.updateCategoryFilter(null) },
                        label = { Text("Tous", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                        modifier = tabModifier,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.15f), 
                            selectedLabelColor = accent,
                            containerColor = Color.Transparent,
                            labelColor = theme.contentColor.copy(alpha = 0.4f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activeFilter == null,
                            borderColor = if (activeFilter == null) accent else theme.contentColor.copy(alpha = 0.1f),
                            borderWidth = 0.8.dp
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    categories.forEach { category ->
                        val isSelected = activeFilter == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateCategoryFilter(category) },
                            label = { Text(category, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                            modifier = tabModifier,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.15f), 
                                selectedLabelColor = accent,
                                containerColor = Color.Transparent,
                                labelColor = theme.contentColor.copy(alpha = 0.4f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) accent else theme.contentColor.copy(alpha = 0.1f),
                                borderWidth = 0.8.dp
                            ),
                            shape = RoundedCornerShape(8.dp)
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
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
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
    val accent = theme.accentColor
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.contentColor.copy(alpha = 0.03f))
            .border(0.5.dp, theme.contentColor.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Médaillon Portrait Premium (v9.7.6)
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(theme.contentColor.copy(alpha = 0.05f))
                .border(2.dp, accent.copy(alpha = 0.3f), CircleShape) // Liseré accent
                .padding(4.dp)
                .border(0.5.dp, accent.copy(alpha = 0.1f), CircleShape) // Double liseré fin
                .clip(CircleShape)
        ) {
            SecureAsyncImage(
                mediaUrl = personality.mainPhotoPath,
                mediaManager = mediaManager,
                creatorId = targetCreatorId,
                explicitKey = heirKey,
                docType = "personalities",
                docId = personality.id,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                isEncrypted = false
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = personality.name,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = theme.fontFamily, // Police plume
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            color = theme.contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(8.dp))

        // Étiquette Catégorie Parchemin/Or (v9.7.6)
        Surface(
            color = theme.contentColor.copy(alpha = 0.05f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(0.5.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Text(
                text = if (personality.category == "Autre") personality.customCategoryLabel ?: "Autre" else personality.category,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = theme.contentColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
