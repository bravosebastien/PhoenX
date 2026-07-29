package com.example.phoenx.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.imageLoader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.drawable.BitmapDrawable
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.components.ProfileDrawer
import com.example.phoenx.ui.components.VideoPlayerBanner
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.PhoenXAvatar
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*
import com.example.phoenx.data.model.PresentationVideo
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun HomeScreen(
    onNavigateToCapture: (String, String?) -> Unit,
    onNavigateToFil: () -> Unit,
    onNavigateToLetters: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTrustCircle: () -> Unit,
    onNavigateToIA: () -> Unit,
    onNavigateToPortraits: () -> Unit,
    onNavigateToWorlds: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToQuestions: () -> Unit,
    onNavigateToPendingQuestions: () -> Unit,
    onNavigateToMailbox: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToBookEditor: () -> Unit,
    onNavigateToDetective: () -> Unit,
    onNavigateToNotificationContacts: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToCube: (String) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onBecomeCreator: () -> Unit,
    onLogoutSuccess: () -> Unit,
    mainViewModel: MainViewModel,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val daysSincePresence by viewModel.daysSincePresence.collectAsState()
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
    val isVideoBannerDismissed by mainViewModel.isVideoBannerDismissed.collectAsState()
    val pendingInvites by mainViewModel.pendingInvitations.collectAsState()
    val isCreator by mainViewModel.isCreator.collectAsState()
    val myRoles by mainViewModel.myRoles.collectAsState()
    val currentPerspective by mainViewModel.currentPerspective.collectAsState()
    
    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedPresentationVideo by remember { mutableStateOf<PresentationVideo?>(null) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = theme.backgroundColor,
            title = { Text("Se déconnecter ?", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Es-tu sûr de vouloir fermer ta session ?", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    mainViewModel.logout()
                    onLogoutSuccess()
                    showLogoutDialog = false
                }) {
                    Text("Déconnexion", color = Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler", color = theme.contentColor)
                }
            }
        )
    }

    if (selectedPresentationVideo != null) {
        PresentationVideoPlayerDialog(
            video = selectedPresentationVideo!!,
            onDismiss = { selectedPresentationVideo = null },
            theme = theme
        )
    }

    ProfileDrawer(
        userName = uiState.userName,
        userEmail = uiState.userEmail,
        photoUrl = uiState.photoUrl,
        onNavigate = { route -> 
            scope.launch { drawerState.close() }
            if (route == "notification_contacts") onNavigateToNotificationContacts()
            if (route == "settings/accessibility") onNavigateToAccessibility()
        },
        onLogout = { 
            scope.launch { drawerState.close() }
            showLogoutDialog = true 
        },
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToTransmission = onNavigateToTrustCircle,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.background(LocalBackgroundBrush.current).statusBarsPadding(),
            bottomBar = {
                HomeNavigationBar(
                    onNavigateToHome = { },
                    onNavigateToTrustCircle = onNavigateToTrustCircle,
                    onNavigateToIA = onNavigateToIA,
                    onOpenProfile = { scope.launch { drawerState.open() } }
                )
            }
        ) { padding ->
            android.util.Log.d("PerspectiveDebug", "HomeScreen: Recomposing with perspective $currentPerspective")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // HEADER
                HomeHeader(
                    name = uiState.userName,
                    photoUrl = uiState.photoUrl,
                    date = uiState.currentDate,
                    onProfileClick = { scope.launch { drawerState.open() } },
                    theme = theme
                )

                if (currentPerspective == MainViewModel.Perspective.MY_MEMORY) {
                    // BANNIÈRE VIDÉO (Remontée v9.2.4)
                    if (!isVideoBannerDismissed) {
                        VideoPlayerBanner(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp)),
                            onDismiss = { mainViewModel.dismissVideoBanner() }
                        )
                    }

                    val welcomeNudge = remember { com.example.phoenx.ui.components.NudgePhrases.getRandomPhrase() }
                    Text(
                        text = welcomeNudge,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontFamily = theme.fontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = theme.contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }

                // SÉLECTEUR DE PERSPECTIVE (v7.7 Multi-rôles)
                if (myRoles.isNotEmpty()) {
                    PerspectiveSwitcher(
                        current = currentPerspective,
                        onSwitch = { mainViewModel.switchPerspective(it) },
                        accent = accent
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (currentPerspective == MainViewModel.Perspective.MY_MEMORY) {
                    // --- AJOUT v9.2 : CARTE MON LIVRE (Positionnée en haut v9.2.3) ---
                    BookCoverCard(
                        title = uiState.bookTitle ?: "Livre de Vie",
                        chaptersCount = uiState.validatedChaptersCount,
                        coverImageUrl = uiState.coverImageUrl,
                        defaultCoverUrl = uiState.defaultCoverUrl,
                        coverTitleStyle = uiState.coverTitleStyle,
                        onClick = onNavigateToBookEditor,
                        theme = theme
                    )

                    // --- VUE CRÉATEUR ---
                    
                    // ALERTE INVITATION EN ATTENTE (v7.6)
                    if (pendingInvites.isNotEmpty()) {
                        Card(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
                            onClick = { onNavigateToTrustCircle() }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.People, null, tint = accent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Tu as ${pendingInvites.size} invitation(s) en attente.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = theme.contentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = accent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // BADGES STATUT
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(
                            title = "Sécurité",
                            subtitle = "Active",
                            dotColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(
                            title = "Présence",
                            subtitle = "il y a $daysSincePresence jours",
                            dotColor = accent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // BOUTONS PRINCIPAUX
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToCapture(Screen.Capture.TYPE_TEXT, null) },
                            modifier = Modifier.weight(1.3f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.25f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Déposer", color = theme.backgroundColor, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(0.85f).height(56.dp).clickable { onNavigateToLibrary() },
                            colors = CardDefaults.cardColors(
                                containerColor = theme.contentColor.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.AutoStories, null, tint = accent, modifier = Modifier.size(18.dp))
                                Text("Ma biblio", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(0.85f).height(56.dp).clickable { onNavigateToFil() },
                            colors = CardDefaults.cardColors(
                                containerColor = theme.contentColor.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.HistoryEdu, null, tint = accent, modifier = Modifier.size(18.dp))
                                Text("Mon Fil", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium))
                            }
                        }
                    }

                    // DERNIER SOUVENIR
                    LastMemoryCard(uiState.latestEntries.firstOrNull())

                // --- AJOUT v9.2.8 : LA TERRE ANIMÉE (Remplace bandeau Mappemonde) ---
                android.util.Log.d("RemoteConfigDebug", "HomeScreen textureUrl: ${uiState.earthTextureUrl}")
                AnimatedEarthCard(
                    textureUrl = uiState.earthTextureUrl,
                    onClick = onNavigateToMap,
                    theme = theme
                )

                    // --- AJOUT v9.2 : MON CERCLE DE CONFIANCE ---
                    TrustCircleCard(
                        onClick = onNavigateToTrustCircle,
                        theme = theme
                    )

                    // --- AJOUT v9.2.6 : GALERIE DE VIDÉOS DE PRÉSENTATION (TOUJOURS VISIBLE v9.2.7) ---
                    PresentationVideoGallery(
                        videos = uiState.presentationVideos,
                        theme = theme,
                        onVideoClick = { video ->
                            selectedPresentationVideo = video
                        }
                    )

                    // ACTIONS RAPIDES
                    Text(
                        "ACTIONS RAPIDES",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = theme.contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        /* Désactivé v9.2.8 : Remplacé par la Terre Animée plus haut
                        QuickActionCard(
                            icon = Icons.Outlined.Public,
                            name = "Mappemonde",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMap
                        )
                        */
                    }

                    // PRÉSENCE (Déplacé ou gardé en bas)
                    Card(
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp).fillMaxWidth().clickable { viewModel.updateProofOfLife() },
                        colors = CardDefaults.cardColors(
                            containerColor = Success.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Success, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Ma présence · confirmée il y a $daysSincePresence jours",
                                color = Success,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                } else {
                    // --- VUE INVITÉ (Héritages) ---
                    com.example.phoenx.ui.screens.universal.GuestPerspectiveContent(
                        myRoles = myRoles,
                        pendingInvites = pendingInvites,
                        isCreator = isCreator ?: true,
                        accent = accent,
                        theme = theme,
                        onNavigateToCube = onNavigateToCube,
                        onAcceptInvite = onAcceptInvite,
                        onBecomeCreator = onBecomeCreator
                    )
                }
            }
        }
    }
}

@Composable
fun PerspectiveSwitcher(
    current: MainViewModel.Perspective,
    onSwitch: (MainViewModel.Perspective) -> Unit,
    accent: Color
) {
    TabRow(
        selectedTabIndex = current.ordinal,
        containerColor = Color.Transparent,
        contentColor = accent,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[current.ordinal]),
                color = accent
            )
        },
        divider = {}
    ) {
        Tab(
            selected = current == MainViewModel.Perspective.MY_MEMORY,
            onClick = { 
                android.util.Log.d("PerspectiveDebug", "HomeScreen: Tab MY_MEMORY clicked")
                onSwitch(MainViewModel.Perspective.MY_MEMORY) 
            },
            text = { Text("MA MÉMOIRE", style = MaterialTheme.typography.labelSmall) }
        )
        Tab(
            selected = current == MainViewModel.Perspective.HERITAGE,
            onClick = { 
                android.util.Log.d("PerspectiveDebug", "HomeScreen: Tab HERITAGE clicked")
                onSwitch(MainViewModel.Perspective.HERITAGE) 
            },
            text = { Text("PROCHES", style = MaterialTheme.typography.labelSmall) }
        )
    }
}

@Composable
fun HomeHeader(name: String, photoUrl: String?, date: String, onProfileClick: () -> Unit, theme: AppThemeState) {
    val accent = theme.accentColor
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bonsoir, $name",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = theme.fontFamily, 
                    fontStyle = FontStyle.Italic, 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor
            )
            Text(
                text = date, 
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                color = theme.contentColor.copy(alpha = 0.5f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoButton(
                title = "Votre Centre de Pilotage",
                points = listOf(
                    "VOTRE LÉGENDE : C'est ici que vous gérez votre héritage émotionnel et numérique.",
                    "LE SILENCE : Votre état de présence est surveillé pour garantir que vos secrets ne seront libérés qu'au bon moment.",
                    "LA BIBLIOTHÈQUE : Accédez à tous vos compartiments (Photos, Vidéos, Secrets, Quiz).",
                    "CERCLE DE CONFIANCE : Gérez qui sont vos héritiers et vos témoins."
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            PhoenXAvatar(
                photoUrl = photoUrl,
                name = name,
                size = 34.dp,
                modifier = Modifier.clickable { onProfileClick() },
                borderColor = accent.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun StatusBadge(title: String, subtitle: String, dotColor: Color, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape).shadow(4.dp, CircleShape, spotColor = dotColor))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    title, 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = theme.fontFamily
                    ), 
                    color = theme.contentColor
                )
                Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = theme.contentColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun LastMemoryCard(entry: com.example.phoenx.data.local.OfflineEntry?) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Card(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(accent, Color.Transparent))))
            Column(modifier = Modifier.padding(13.dp, 14.dp)) {
                Text(
                    "DERNIER SOUVENIR", 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), 
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (entry == null) {
                    Text(
                        "Aucun souvenir déposé pour l'instant.", 
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic, 
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp
                        ), 
                        color = theme.contentColor.copy(alpha = 0.7f)
                    )
                    Text("— Commence dès maintenant", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                } else {
                    Text(
                        entry.aiSummary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic, 
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp
                        ),
                        color = theme.contentColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val age = com.example.phoenx.domain.util.AgeUtils.parseAgeJson(entry.ageAtCreation)
                    Text("— À ${age.years} ans", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun ProgressionCard(memoriesCount: Int, questionsCount: Int, chaptersCount: Int) {
    val theme = LocalAppTheme.current
    Card(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(count = memoriesCount, label = "SOUVENIRS", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.contentColor.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
            StatItem(count = questionsCount, label = "QUESTIONS", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.contentColor.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
            StatItem(count = chaptersCount, label = "CHAPITRES", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(count: Int, label: String, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(), 
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = theme.fontFamily, 
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ), 
            color = accent
        )
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), 
            color = theme.contentColor.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    name: String, 
    modifier: Modifier = Modifier,
    badgeCount: Int = 0, 
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Card(
        modifier = modifier.height(80.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(accent.copy(alpha = 0.4f)))
            
            if (badgeCount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp),
                    shape = CircleShape,
                    color = accent
                ) {
                    Text(badgeCount.toString(), color = theme.backgroundColor, fontSize = 8.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    name, 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = theme.fontFamily,
                        fontStyle = FontStyle.Italic, 
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ), 
                    color = theme.contentColor.copy(alpha = 0.8f), 
                    textAlign = TextAlign.Center
                )
            }
            
            // Halo bottom
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp).background(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.05f)))))
        }
    }
}

@Composable
fun HomeNavigationBar(
    onNavigateToHome: () -> Unit,
    onNavigateToTrustCircle: () -> Unit,
    onNavigateToIA: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val theme = LocalAppTheme.current
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = theme.backgroundColor.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(Icons.Outlined.Home, "Accueil", true, onNavigateToHome)
            NavItem(Icons.Outlined.People, "Mon Cercle", false, onNavigateToTrustCircle)
            NavItem(Icons.Outlined.AutoAwesome, "L'IA", false, onNavigateToIA)
            NavItem(Icons.Outlined.AccountCircle, "Profil", false, onOpenProfile)
        }
    }
}

@Composable
fun BookCoverCard(
    title: String,
    chaptersCount: Int,
    coverImageUrl: String? = null,
    defaultCoverUrl: String? = null,
    coverTitleStyle: String = "GOLD",
    onClick: () -> Unit,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    val finalCoverUrl = coverImageUrl ?: defaultCoverUrl
    val hasBackgroundImage = finalCoverUrl != null
    
    val titleBrush = getTitleBrush(coverTitleStyle)
    val titleColor = when(coverTitleStyle) {
        "BLACK" -> Color.Black.copy(alpha = 0.85f)
        "WHITE" -> Color.White.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.85f)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            "MON LIVRE",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )
        
        Card(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(180.dp)
                .aspectRatio(0.72f)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = accent.copy(alpha = 0.5f),
                    ambientColor = accent.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.6f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // FOND : Image personnalisée ou par défaut Remote Config (v9.2.5)
                if (hasBackgroundImage) {
                    coil3.compose.AsyncImage(
                        model = finalCoverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Voile sombre progressif réduit pour la visibilité du livre (v9.2.7)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                } else {
                    // 3. Stylized Drawing (Fallback v9.2.6)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.copy(alpha = 0.02f), accent.copy(alpha = 0.08f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).alpha(0.05f),
                            tint = accent
                        )
                    }
                }

                // Tranche stylisée (v9.2.1: Renforcée)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(14.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    (if (hasBackgroundImage) Color.Black else accent).copy(alpha = 0.4f),
                                    (if (hasBackgroundImage) Color.Black else accent).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), // v9.2.7 : Top quasi-collé
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    var fontSize by remember(title) { mutableStateOf(20.sp) }
                    
                    val baseTextStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = theme.fontFamily,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic,
                        lineHeight = (fontSize.value * 1.2).sp,
                        shadow = if (hasBackgroundImage || coverTitleStyle != "BLACK") androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.4f),
                            offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                            blurRadius = 4f
                        ) else null
                    )

                    Text(
                        text = title,
                        onTextLayout = { result ->
                            // v9.2.3 : Réduction plus agressive pour tablette et gros réglages police
                            if (result.hasVisualOverflow && fontSize > 10.sp) {
                                fontSize = (fontSize.value - 1).sp
                            }
                        },
                        style = if (titleBrush != null) baseTextStyle.copy(brush = titleBrush) 
                                else baseTextStyle.copy(color = titleColor),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.5.dp)
                            .background((if (hasBackgroundImage) Color.White else accent).copy(alpha = 0.4f))
                    )
                    
                    // Suppression "X chapitres validés" (v9.2.7)
                }
                
                // Effet de relief sur le bord droit
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, accent.copy(alpha = 0.05f))
                            )
                        )
                )
            }
        }
    }
}

fun getTitleBrush(style: String): Brush? {
    val alpha = 0.82f // v9.2.7 : Rendu plus doux
    return when (style) {
        "GOLD" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8860B).copy(alpha = alpha), 
                Color(0xFFFFF8DC).copy(alpha = alpha), 
                Color(0xFFB8860B).copy(alpha = alpha)
            )
        )
        "SILVER" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFA8A9AD).copy(alpha = alpha), 
                Color(0xFFFFFFFF).copy(alpha = alpha), 
                Color(0xFFA8A9AD).copy(alpha = alpha)
            )
        )
        "EMERALD" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF046307).copy(alpha = alpha), 
                Color(0xFF50C878).copy(alpha = alpha), 
                Color(0xFF046307).copy(alpha = alpha)
            )
        )
        "RED" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A0404).copy(alpha = alpha), 
                Color(0xFFB22222).copy(alpha = alpha), 
                Color(0xFF7B0323).copy(alpha = alpha)
            )
        )
        else -> null
    }
}

@Composable
fun TrustCircleCard(
    onClick: () -> Unit,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(110.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.People, null, tint = accent, modifier = Modifier.size(26.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Mon Cercle de Confiance",
                    style = TextStyle(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = theme.contentColor
                )
                Text(
                    "Gérer mes héritiers et dépositaires",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = theme.contentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PresentationVideoGallery(
    videos: List<PresentationVideo>,
    theme: AppThemeState,
    onVideoClick: (PresentationVideo) -> Unit
) {
    val accent = theme.accentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Text(
            "VOTRE GUIDE VIDÉO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
        )

        // Grille fixe de 6 slots (3 par ligne, 2 lignes) (v9.2.7)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            repeat(2) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) { colIndex ->
                        val slotNumber = rowIndex * 3 + colIndex + 1 // 1 à 6
                        val video = videos.find { it.slotIndex == slotNumber }
                        
                        Column(
                            modifier = Modifier
                                .width(90.dp)
                                .then(if (video != null) Modifier.clickable { onVideoClick(video) } else Modifier),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(theme.contentColor.copy(alpha = 0.04f))
                                    .border(
                                        width = 1.dp,
                                        color = if (video != null) accent.copy(alpha = 0.4f) else theme.contentColor.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (video?.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = video.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (video != null) {
                                    Icon(Icons.Outlined.PlayArrow, null, tint = accent.copy(alpha = 0.6f))
                                } else {
                                    // Placeholder vide discret (v9.2.7)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(theme.contentColor.copy(alpha = 0.05f), CircleShape)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = video?.title ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = theme.contentColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.heightIn(min = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun PresentationVideoPlayerDialog(
    video: PresentationVideo,
    onDismiss: () -> Unit,
    theme: AppThemeState
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(video.title, color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, null, tint = Color.White)
                    }
                }
                
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    VideoPlayerBanner(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16/9f),
                        overrideVideoUrl = video.videoUrl, // v9.2.7 : Lecture de l'URL spécifique
                        onDismiss = {} // Non applicable ici
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedEarthCard(
    textureUrl: String?,
    onClick: () -> Unit,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    val context = LocalContext.current
    var earthBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Chargement manuel unique du Bitmap (v9.2.9)
    LaunchedEffect(textureUrl) {
        if (textureUrl != null) {
            try {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(textureUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                
                val result = loader.execute(request)
                if (result is coil3.request.SuccessResult) {
                    // Coil 3: Extraction du bitmap depuis l'objet Image
                    val bitmap = (result.image as? coil3.BitmapImage)?.bitmap
                    earthBitmap = bitmap?.asImageBitmap()
                    android.util.Log.d("EarthDebug", "Manual bitmap load successful: ${earthBitmap != null}")
                }
            } catch (e: Exception) {
                android.util.Log.e("EarthDebug", "Manual bitmap load failed: ${e.message}")
            }
        }
    }

    // Animation de rotation (Amplitude calée sur 300dp)
    val infiniteTransition = rememberInfiniteTransition(label = "earthRotation")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f, 
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ombre portée circulaire
        Box(
            modifier = Modifier
                .size(130.dp)
                .shadow(20.dp, CircleShape, spotColor = accent.copy(alpha = 0.3f))
        )

        // Sphère
        Card(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(0.5.dp, accent.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                // Fond Marin
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        0.0f to Color(0xFF001D3D),
                        1.0f to Color(0xFF000814)
                    )
                ))

                if (earthBitmap != null) {
                    // Dessin direct au Canvas pour éliminer les rechargements Coil (v9.2.9)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val textureWidthPx = 300.dp.toPx()
                        val currentOffsetPx = offset.dp.toPx()
                        
                        // Instance 1
                        drawImage(
                            image = earthBitmap!!,
                            dstOffset = androidx.compose.ui.unit.IntOffset(currentOffsetPx.toInt(), 0),
                            dstSize = androidx.compose.ui.unit.IntSize(textureWidthPx.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.tint(accent.copy(alpha = 0.6f), BlendMode.SrcAtop)
                        )
                        // Instance 2 (Boucle infinie)
                        drawImage(
                            image = earthBitmap!!,
                            dstOffset = androidx.compose.ui.unit.IntOffset((currentOffsetPx + textureWidthPx).toInt(), 0),
                            dstSize = androidx.compose.ui.unit.IntSize(textureWidthPx.toInt(), size.height.toInt()),
                            colorFilter = ColorFilter.tint(accent.copy(alpha = 0.6f), BlendMode.SrcAtop)
                        )
                    }
                } else {
                    // Fallback procédural si Bitmap non prêt
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(400.dp)
                            .offset(x = (offset * (200f/300f)).dp)
                    ) {
                        EarthTexture()
                        EarthTexture()
                    }
                }

                // Effet de relief et Halo (Superposés)
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.12f),
                        0.4f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.8f),
                        center = androidx.compose.ui.geometry.Offset(90f, 90f)
                    )
                ))
                
                Box(modifier = Modifier.fillMaxSize().border(
                    width = 1.2.dp,
                    brush = Brush.radialGradient(
                        0.0f to accent.copy(alpha = 0.5f),
                        1.0f to Color.Transparent
                    ),
                    shape = CircleShape
                ))
            }
        }
    }
}

@Composable
fun EarthTextureImage(painter: androidx.compose.ui.graphics.painter.Painter, tint: Color) {
    androidx.compose.foundation.Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(
            tint.copy(alpha = 0.6f),
            BlendMode.SrcAtop
        )
    )
}

@Composable
fun EarthTexture() {
    // Dégradé pour les continents (Relief suggéré)
    val continentBrush = Brush.verticalGradient(
        listOf(Color(0xFF52796F), Color(0xFF84A98C), Color(0xFF354F52))
    )
    
    Canvas(modifier = Modifier.width(200.dp).fillMaxHeight()) {
        // AMÉRIQUE DU NORD
        val northAmerica = androidx.compose.ui.graphics.Path().apply {
            moveTo(10.dp.toPx(), 20.dp.toPx())
            lineTo(45.dp.toPx(), 15.dp.toPx())
            lineTo(55.dp.toPx(), 35.dp.toPx())
            lineTo(35.dp.toPx(), 45.dp.toPx())
            lineTo(25.dp.toPx(), 40.dp.toPx())
            close()
        }
        drawPath(northAmerica, continentBrush)

        // AMÉRIQUE DU SUD
        val southAmerica = androidx.compose.ui.graphics.Path().apply {
            moveTo(35.dp.toPx(), 48.dp.toPx())
            lineTo(50.dp.toPx(), 55.dp.toPx())
            lineTo(40.dp.toPx(), 85.dp.toPx())
            lineTo(30.dp.toPx(), 65.dp.toPx())
            close()
        }
        drawPath(southAmerica, continentBrush)

        // EURASIE
        val eurasia = androidx.compose.ui.graphics.Path().apply {
            moveTo(95.dp.toPx(), 15.dp.toPx())
            lineTo(165.dp.toPx(), 10.dp.toPx())
            lineTo(180.dp.toPx(), 30.dp.toPx())
            lineTo(150.dp.toPx(), 45.dp.toPx())
            lineTo(110.dp.toPx(), 35.dp.toPx())
            close()
        }
        drawPath(eurasia, continentBrush)

        // AFRIQUE
        val africa = androidx.compose.ui.graphics.Path().apply {
            moveTo(105.dp.toPx(), 38.dp.toPx())
            lineTo(135.dp.toPx(), 42.dp.toPx())
            lineTo(130.dp.toPx(), 65.dp.toPx())
            lineTo(115.dp.toPx(), 75.dp.toPx())
            lineTo(100.dp.toPx(), 55.dp.toPx())
            close()
        }
        drawPath(africa, continentBrush)

        // AUSTRALIE
        val australia = androidx.compose.ui.graphics.Path().apply {
            moveTo(160.dp.toPx(), 65.dp.toPx())
            lineTo(185.dp.toPx(), 68.dp.toPx())
            lineTo(180.dp.toPx(), 82.dp.toPx())
            lineTo(155.dp.toPx(), 78.dp.toPx())
            close()
        }
        drawPath(australia, continentBrush)

        // QUELQUES ÎLES (Groenland, Japon, UK)
        drawCircle(continentBrush, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(45.dp.toPx(), 10.dp.toPx()))
        drawCircle(continentBrush, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(175.dp.toPx(), 22.dp.toPx()))
        drawCircle(continentBrush, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(105.dp.toPx(), 18.dp.toPx()))
    }
}

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (active) accent else theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = if (active) accent else theme.contentColor.copy(alpha = 0.3f))
    }
}
