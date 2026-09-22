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
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.example.phoenx.R
import com.example.phoenx.ui.components.CompartmentMediaIcon
import com.example.phoenx.ui.components.OnboardingPopup
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
    mediaViewModel: com.example.phoenx.ui.screens.recipient.RecipientMediaViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val libraryEntries by mediaViewModel.libraryEntries.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    OnboardingPopup(
        pageKey = "library",
        title = stringResource(R.string.library_my_library),
        contentPoints = listOf(
            stringResource(R.string.library_onboarding_point_1),
            stringResource(R.string.library_onboarding_point_2)
        ),
        preferenceManager = themeViewModel.preferenceManager
    )

    val videoEntries by mediaViewModel.videoEntries.collectAsState()
    val discothequeEntries by mediaViewModel.discothequeEntries.collectAsState()
    val archiveEntries by mediaViewModel.archiveEntries.collectAsState()
    
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(targetCreatorId) {
        mediaViewModel.setTargetCreator(targetCreatorId)
        viewModel.setTargetCreator(targetCreatorId)
    }

    // Filtrer pour ne compter que les souvenirs racines (v8.3.4)
    val rootLibrary = libraryEntries.filter { it.parentEntryId == null }
    val rootVideo = videoEntries.filter { it.parentEntryId == null }
    val rootDisco = discothequeEntries.filter { it.parentEntryId == null }
    val rootArchive = archiveEntries.filter { it.parentEntryId == null }
    
    val bookTitle by mediaViewModel.bookTitle.collectAsState()
    val totalSouvenirs = rootLibrary.size + rootVideo.size + rootDisco.size + rootArchive.size
    val compartmentsConfig by viewModel.compartmentsConfig.collectAsState()

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
                text = stringResource(R.string.library_my_library),
                style = TextStyle(
                    fontFamily = theme.fontFamily, 
                    fontStyle = FontStyle.Italic, 
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                ),
                color = theme.contentColor
            )
            Spacer(modifier = Modifier.width(48.dp)) // Compensation visuelle pour centrage relatif
        }

        Text(
            text = stringResource(R.string.library_stats_label, 14, totalSouvenirs),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = theme.contentColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp, bottom = 14.dp)
        )

        // ── 1. ESSENTIELS (Lignes fines - AGRANDIES v8.9.6) ──────────────────
        Text(
            stringResource(R.string.library_essentials),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
        )

        CompactEssentialRow(
            title = stringResource(R.string.library_fil_pensee_title),
            info = stringResource(R.string.library_fil_pensee_info, totalSouvenirs),
            icon = Icons.Outlined.Timeline,
            onClick = { 
                val route = if (isCreatorMode) "fil_pensee" else "fil_pensee?creatorId=$targetCreatorId"
                navController.navigate(route) 
            },
            theme = theme,
            mediaUrl = compartmentsConfig.filPenseeUrl
        )

        CompactEssentialRow(
            title = bookTitle ?: stringResource(R.string.library_book_ma_vie_title),
            info = if (isCreatorMode) stringResource(R.string.library_book_info_creator) else stringResource(R.string.library_book_info_heir),
            icon = Icons.Outlined.MenuBook,
            onClick = { 
                if (isCreatorMode) {
                    navController.navigate("book_editor") 
                } else {
                    navController.navigate("book_viewer_recipient?creatorId=$targetCreatorId")
                }
            },
            theme = theme,
            mediaUrl = compartmentsConfig.livreMaVieUrl
        )

        Spacer(modifier = Modifier.height(64.dp))

        // ── 2. GRILLE DE 6 BLOCS VISIBLES (AGRANDIS v8.9.6) ──────────────────
        Text(
            stringResource(R.string.library_compartments),
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
                label = stringResource(R.string.library_videotheque),
                icon = Icons.Outlined.Movie,
                onClick = { navController.navigate(Screen.RecipientVideotheque.createRoute(targetCreatorId ?: mediaViewModel.currentUid)) },
                theme = theme,
                modifier = itemModifier,
                mediaUrl = compartmentsConfig.videothequeUrl
            )
            CompactGridItem(
                label = stringResource(R.string.library_phototheque),
                icon = Icons.Outlined.PhotoCamera,
                onClick = { navController.navigate(Screen.RecipientPhotos.createRoute(targetCreatorId ?: mediaViewModel.currentUid)) },
                theme = theme,
                modifier = itemModifier,
                mediaUrl = compartmentsConfig.photothequeUrl
            )
            CompactGridItem(
                label = stringResource(R.string.library_discotheque),
                icon = Icons.Outlined.Album,
                onClick = { navController.navigate(Screen.RecipientDiscotheque.createRoute(targetCreatorId ?: mediaViewModel.currentUid)) },
                theme = theme,
                modifier = itemModifier,
                mediaUrl = compartmentsConfig.discothequeUrl
            )
            CompactGridItem(
                label = stringResource(R.string.library_mappemonde),
                icon = Icons.Outlined.Public,
                onClick = { navController.navigate(Screen.Map.createRoute(targetCreatorId = targetCreatorId ?: mediaViewModel.currentUid)) },
                theme = theme,
                modifier = itemModifier,
                mediaUrl = compartmentsConfig.mappemondeUrl
            )
            CompactGridItem(
                label = stringResource(R.string.library_le_litteraire),
                icon = Icons.Outlined.AutoStories,
                onClick = { navController.navigate("literary_library?creatorId=${targetCreatorId ?: mediaViewModel.currentUid}") },
                theme = theme,
                modifier = itemModifier,
                mediaUrl = compartmentsConfig.litteraireUrl
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
                    text = if (isExpanded) stringResource(R.string.library_reduce) else stringResource(R.string.library_others),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = accent,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── 3. ZONE DÉPLIÉE ───────────────────────────────
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                
                Text(
                    stringResource(R.string.library_other_compartments),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                    color = theme.contentColor.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 8.dp, top = 32.dp, bottom = 12.dp)
                )

                val itemModifier = Modifier.weight(1f)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3
                ) {
                    CompactGridItem(stringResource(R.string.library_personalities), Icons.Outlined.Star, { 
                        val route = if (isCreatorMode) "personalities" else "personalities?creatorId=$targetCreatorId"
                        navController.navigate(route) 
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.personalitiesUrl)

                    /* Haché v12.3 (Réversible)
                    CompactGridItem("Mon Quiz", Icons.Outlined.EmojiEvents, {
                        if (isCreatorMode) navController.navigate("quiz_create")
                    }, theme, itemModifier)
                    */

                    CompactGridItem(stringResource(R.string.library_capsule_temporelle), Icons.Outlined.MailOutline, { 
                        val route = if (isCreatorMode) "lettres" else Screen.RecipientMailbox.createRoute(targetCreatorId)
                        navController.navigate(route)
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.capsuleTemporelleUrl)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3
                ) {
                    CompactGridItem(stringResource(R.string.library_reconciliation), Icons.Outlined.Mail, { 
                        navController.navigate("reconciliation") 
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.reconciliationUrl)

                    CompactGridItem(stringResource(R.string.library_miroir_deux), Icons.Outlined.Handshake, { 
                        navController.navigate("le_pacte") 
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.miroirDeuxUrl)

                    CompactGridItem(stringResource(R.string.library_mes_classements), Icons.Outlined.FormatListNumbered, { 
                        navController.navigate(Screen.Rankings.createRoute(targetCreatorId))
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.mesClassementsUrl)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3
                ) {
                    CompactGridItem(stringResource(R.string.library_100_questions), Icons.Outlined.HelpOutline, { 
                        navController.navigate(Screen.Questions.createRoute(targetCreatorId))
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.centQuestionsUrl)

                    CompactGridItem(stringResource(R.string.library_portraits), Icons.Outlined.AccountCircle, { 
                        navController.navigate("portrait_proche") 
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.portraitProcheUrl)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3
                ) {
                    CompactGridItem(stringResource(R.string.library_lettre_a_moi), Icons.Outlined.HistoryEdu, {
                        navController.navigate("youngselfletters") 
                    }, theme, itemModifier, mediaUrl = compartmentsConfig.lettreAMoiUrl)
                    
                    // Remplissage
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
    theme: AppThemeState,
    mediaUrl: String? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompartmentMediaIcon(
                mediaUrl = mediaUrl,
                fallbackIcon = icon,
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                iconSize = 28.dp,
                containerColor = theme.accentColor.copy(alpha = 0.1f),
                iconTint = theme.accentColor,
                borderColor = theme.contentColor.copy(alpha = 0.1f)
            )
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
    modifier: Modifier = Modifier,
    mediaUrl: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CompartmentMediaIcon(
            mediaUrl = mediaUrl,
            fallbackIcon = icon,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            iconSize = 32.dp,
            containerColor = theme.contentColor.copy(alpha = 0.04f),
            iconTint = theme.accentColor,
            borderColor = theme.contentColor.copy(alpha = 0.1f)
        )
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
