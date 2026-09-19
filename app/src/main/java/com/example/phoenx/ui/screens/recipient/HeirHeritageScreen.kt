package com.example.phoenx.ui.screens.recipient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import coil3.compose.AsyncImage
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.components.LoopingVideoBackground
import com.example.phoenx.ui.components.isVideoUrl
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.screens.home.components.AnimatedEarthCard
import com.example.phoenx.ui.screens.home.components.BookCoverCard
import com.example.phoenx.ui.screens.home.components.EncounterCard
import com.example.phoenx.ui.screens.home.components.GenealogyCard
import com.example.phoenx.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.media3.common.util.UnstableApi

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun HeirHeritageScreen(
    creatorId: String,
    navController: NavController,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val heritageEntries by viewModel.heritageEntries.collectAsState()
    val heirKey by viewModel.heirKey.collectAsState()
    val bookTitle by viewModel.bookTitle.collectAsState()
    val creatorName by viewModel.creatorName.collectAsState()
    val protocolStatus by viewModel.protocolStatus.collectAsState()
    val canAsk by viewModel.canAskQuestions.collectAsState()
    val maxQuestions by viewModel.maxQuestions.collectAsState()
    val questionsAsked by viewModel.questionsAsked.collectAsState()
    val recipientId by viewModel.recipientId.collectAsState()
    val ambiance by viewModel.ambiance.collectAsState()
    val isMirrorRevealed by viewModel.isMirrorRevealed.collectAsState()
    val mirrorId by viewModel.mirrorId.collectAsState()

    // v12.4 : Visuels réutilisés depuis l'accueil du Créateur
    val bookCoverImageUrl by viewModel.bookCoverImageUrl.collectAsState()
    val bookCoverIsVideo by viewModel.bookCoverIsVideo.collectAsState()
    val bookCoverTitleStyle by viewModel.bookCoverTitleStyle.collectAsState()
    val bookCoverScale by viewModel.bookCoverScale.collectAsState()
    val bookCoverOffsetX by viewModel.bookCoverOffsetX.collectAsState()
    val bookCoverOffsetY by viewModel.bookCoverOffsetY.collectAsState()
    val defaultBookCoverUrl by viewModel.defaultBookCoverUrl.collectAsState()
    val genealogyCardImageUrl by viewModel.genealogyCardImageUrl.collectAsState()
    val encountersCardImageUrl by viewModel.encountersCardImageUrl.collectAsState()
    val earthTextureUrl by viewModel.earthTextureUrl.collectAsState()
    val legacyVideoUrl by viewModel.legacyVideoUrl.collectAsState()

    // v12.4 : La liste à plat des souvenirs est maintenant repliée derrière une tuile "Liste de souvenirs"
    var showSouvenirsList by remember { mutableStateOf(false) }

    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    TransmissionTheme(
        backgroundId = ambiance.backgroundId,
        fontId = ambiance.fontId
    ) {
        val theme = LocalAppTheme.current
        Scaffold(
            containerColor = theme.backgroundColor,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.heir_heritage_screen_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                            color = theme.contentColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                        }
                    },
                    actions = {
                        if (canAsk && recipientId != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                                IconButton(
                                    onClick = { navController.navigate(Screen.AskQuestion.createRoute(creatorId, recipientId!!)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.heir_heritage_ask_question_desc), tint = accent)
                                }
                                if (maxQuestions != null) {
                                    val remaining = (maxQuestions!! - questionsAsked).coerceAtLeast(0)
                                    Text(
                                        text = stringResource(R.string.heir_heritage_remaining_questions, remaining),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = accent.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 0. VIDÉO DE PRÉSENTATION DE L'HÉRITAGE (v12.6) — n'apparaît que si le Créateur en a déposé une
                if (legacyVideoUrl != null) {
                    item {
                        com.example.phoenx.ui.components.VideoPlayerBanner(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            overrideVideoUrl = legacyVideoUrl,
                            onDismiss = {}
                        )
                    }
                }

                // 1. LES PILIERS DE L'HÉRITAGE (v12.5 : Grille 2x2 Harmonisée - Point 2 & 3)
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // MAPPEMONDE
                            HeritageMainCard(
                                title = stringResource(R.string.heir_heritage_main_map),
                                modifier = Modifier.weight(1f),
                                theme = theme,
                                content = {
                                    AnimatedEarthCard(
                                        textureUrl = earthTextureUrl,
                                        onClick = { navController.navigate(Screen.Map.createRoute(targetCreatorId = creatorId)) },
                                        theme = theme
                                    )
                                }
                            ) { navController.navigate(Screen.Map.createRoute(targetCreatorId = creatorId)) }

                            // GÉNÉALOGIE
                            HeritageMainCard(
                                title = stringResource(R.string.heir_heritage_main_tree),
                                modifier = Modifier.weight(1f),
                                theme = theme,
                                content = {
                                    if (genealogyCardImageUrl != null) {
                                        if (isVideoUrl(genealogyCardImageUrl)) {
                                            LoopingVideoBackground(
                                                videoUrl = genealogyCardImageUrl!!,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            AsyncImage(
                                                model = genealogyCardImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            ) { navController.navigate(Screen.Genealogy.createRoute(creatorId)) }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // RENCONTRES
                            HeritageMainCard(
                                title = stringResource(R.string.heir_heritage_main_encounters),
                                modifier = Modifier.weight(1f),
                                theme = theme,
                                content = {
                                    if (encountersCardImageUrl != null) {
                                        if (isVideoUrl(encountersCardImageUrl)) {
                                            LoopingVideoBackground(
                                                videoUrl = encountersCardImageUrl!!,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            AsyncImage(
                                                model = encountersCardImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            ) { navController.navigate(Screen.RecipientEncounters.createRoute(creatorId)) }

                            // LIVRE DE VIE (v12.5 : Intégré dans la grille avec FIX Couverture)
                            val titleText = bookTitle ?: stringResource(R.string.heir_heritage_book_default_title)
                            HeritageMainCard(
                                title = titleText,
                                modifier = Modifier.weight(1f),
                                theme = theme,
                                content = {
                                    // Utilisation de la version qui gère explicitKey pour le Destinataire
                                    BookCoverCard(
                                        title = titleText,
                                        chaptersCount = 0,
                                        coverImageUrl = bookCoverImageUrl, // v12.5 : Résolu réactivement dans le VM
                                        coverIsVideo = bookCoverIsVideo,
                                        defaultCoverUrl = defaultBookCoverUrl,
                                        coverTitleStyle = bookCoverTitleStyle,
                                        scale = bookCoverScale,
                                        offsetX = bookCoverOffsetX,
                                        offsetY = bookCoverOffsetY,
                                        theme = theme,
                                        explicitKey = heirKey, // Fix critique Bug 1
                                        creatorId = creatorId, // v12.6.1 : requis pour résolution sécurisée Destinataire
                                        isCompact = true,
                                        onClick = {
                                            if (protocolStatus == RecipientMediaViewModel.ProtocolStatus.ACTIVATED) {
                                                // v13.0 : Ouvre directement la lecture du Livre (au lieu de la Bibliothèque générale)
                                                navController.navigate("book_viewer_recipient?creatorId=$creatorId")
                                            }
                                        }
                                    )
                                }
                            ) {
                                if (protocolStatus == RecipientMediaViewModel.ProtocolStatus.ACTIVATED) {
                                    navController.navigate("book_viewer_recipient?creatorId=$creatorId")
                                }
                            }
                        }
                    }
                }

                // 2. LE COFFRET DES SOUVENIRS (v12.5 : Habillage "Coffret Ancien" - Point 1, 2, 3)
                item {
                    HeritageCasket(
                        title = "SOUVENIRS",
                        isExpanded = showSouvenirsList,
                        onToggle = { showSouvenirsList = !showSouvenirsList },
                        theme = theme,
                        accent = accent
                    ) {
                        // Grille de vignettes rondes (3 colonnes)
                        data class CasketItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)
                        val casketItems = listOf(
                            CasketItem(stringResource(R.string.heir_heritage_special_vault_title), Icons.Outlined.Lock) { navController.navigate(Screen.RecipientDetective.createRoute(creatorId)) },
                            CasketItem(stringResource(R.string.heir_heritage_special_personalities_title), Icons.Default.Star) { navController.navigate(Screen.Personalities.createRoute(creatorId)) },
                            CasketItem(stringResource(R.string.heir_heritage_special_photos_title), Icons.Default.PhotoLibrary) { navController.navigate(Screen.RecipientPhotos.createRoute(creatorId)) },
                            CasketItem(stringResource(R.string.heir_heritage_special_videos_title), Icons.Default.Videocam) { navController.navigate(Screen.RecipientVideotheque.createRoute(creatorId)) },
                            CasketItem(stringResource(R.string.heir_heritage_special_audios_title), Icons.Default.MusicNote) { navController.navigate(Screen.RecipientDiscotheque.createRoute(creatorId)) },
                            CasketItem(stringResource(R.string.questions_title), Icons.Default.QuestionAnswer) { navController.navigate(Screen.HundredQuestionsLeaderboard.createRoute(creatorId)) },
                            CasketItem(stringResource(R.string.library_mes_classements), Icons.Default.FormatListNumbered) { navController.navigate(Screen.Rankings.createRoute(creatorId)) },
                            CasketItem(stringResource(R.string.library_capsule_temporelle), Icons.Default.MailOutline) { navController.navigate("lettres") }
                        )

                        // Si Miroir révélé, on l'ajoute
                        val finalItems = if (isMirrorRevealed) {
                            casketItems + CasketItem(stringResource(R.string.heir_heritage_special_mirror_title), Icons.Default.People) {
                                mirrorId?.let { navController.navigate(Screen.RecipientPact.createRoute(it)) }
                            }
                        } else casketItems

                        Column(modifier = Modifier.padding(16.dp)) {
                            finalItems.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        CasketThumbnail(
                                            label = item.label,
                                            icon = item.icon,
                                            onClick = item.onClick,
                                            theme = theme,
                                            accent = accent,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // Compléter la ligne si nécessaire
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // LISTE DES SOUVENIRS (v12.4 : repliée par défaut, dépliée via le coffret)
                if (showSouvenirsList) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(heritageEntries) { entry ->
                        HeritageEntryRow(
                            entry = entry,
                            heirKey = heirKey,
                            mediaManager = viewModel.mediaManager,
                            theme = theme,
                            creatorId = creatorId, // v9.4.27
                            onClick = {
                                // v9.4.27 : Gestion intelligente du clic (Direct ou Détail)
                                // On vérifie à la fois mediaUrl et le contenu déchiffré
                                val contentStr = String(entry.encryptedContent)
                                val url = if (entry.mediaUrl?.startsWith("http") == true) entry.mediaUrl else contentStr

                                val isExternal = url.startsWith("http") &&
                                        (entry.mediaProvider != null || url.contains("spotify") || url.contains("youtube") || url.contains("deezer"))

                                if (isExternal) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch(_: Exception) {
                                        navController.navigate("recipient_memory_detail/${entry.id}/$creatorId")
                                    }
                                } else {
                                    navController.navigate("recipient_memory_detail/${entry.id}/$creatorId")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeritageCasket(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    theme: AppThemeState,
    accent: Color,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = theme.contentColor.copy(alpha = 0.02f),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.4f))
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(theme.backgroundColor, accent.copy(alpha = 0.12f))
                        )
                    )
                    .clickable { onToggle() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = theme.contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        null,
                        tint = accent.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )
                }
                // Bordure basse du header
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(accent.copy(alpha = 0.2f))
                )
            }

            // Body
            AnimatedVisibility(visible = isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun CasketThumbnail(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    theme: AppThemeState,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(theme.backgroundColor, accent.copy(alpha = 0.15f))
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = theme.contentColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun HeritageEntryRow(
    entry: PhoenXEntry,
    heirKey: ByteArray?,
    mediaManager: MediaManager,
    theme: AppThemeState,
    creatorId: String, // v9.4.27
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    val dateFormat = stringResource(R.string.heir_heritage_date_format)
    val dateFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat, Locale.FRENCH).withZone(ZoneId.systemDefault()) }
    val formattedDate = dateFormatter.format(entry.timestamp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // MINIATURE MÉDIA
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.contentColor.copy(alpha = 0.05f))
            ) {
                if (entry.type == EntryType.PHOTO) {
                    SecureAsyncImage(
                        mediaUrl = entry.mediaUrl,
                        localPath = entry.localMediaPath,
                        explicitKey = heirKey,
                        mediaManager = mediaManager,
                        modifier = Modifier.fillMaxSize(),
                        creatorId = creatorId,           // v9.4.27
                        docType = entry.sourceDocType,   // v9.4.27
                        docId = entry.id                 // v9.4.27
                    )
                } else {
                    val icon = when(entry.type) {
                        EntryType.AUDIO -> Icons.Default.Mic
                        EntryType.VIDEO -> Icons.Default.Videocam
                        else -> Icons.Default.Description
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.aiSummary.ifEmpty { stringResource(R.string.heir_heritage_entry_fallback_title) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Récit tronqué à 4 lignes (Point 2)
                Text(
                    text = String(entry.encryptedContent),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = theme.contentColor.copy(alpha = 0.7f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun HeritageMainCard(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    theme: AppThemeState,
    content: @Composable (BoxScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (content != null) {
                content()
            } else if (icon != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        icon, null,
                        tint = accent.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Bottom Label
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
                    .padding(8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
