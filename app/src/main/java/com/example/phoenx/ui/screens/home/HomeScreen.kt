package com.example.phoenx.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.phoenx.ui.screens.home.components.AnimatedEarthCard
import com.example.phoenx.ui.screens.home.components.BookCoverCard
import com.example.phoenx.ui.screens.home.components.GenealogyCard
import com.example.phoenx.ui.screens.home.components.HomeHeader
import com.example.phoenx.ui.screens.home.components.HomeNavigationBar
import com.example.phoenx.ui.screens.home.components.LastMemoryCard
import com.example.phoenx.ui.screens.home.components.PerspectiveSwitcher
import com.example.phoenx.ui.screens.home.components.PresentationVideoGallery
import com.example.phoenx.ui.screens.home.components.PresentationVideoPlayerDialog
import com.example.phoenx.ui.screens.home.components.QuickActionCard
import com.example.phoenx.ui.screens.home.components.StatusBadge
import com.example.phoenx.ui.screens.home.components.TrustCircleCard
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
    onNavigateToGenealogy: () -> Unit,
    onNavigateToDetective: () -> Unit,
    onNavigateToStepByStep: () -> Unit, // v9.4.26
    onNavigateToNotificationContacts: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToCube: (String) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onBecomeCreator: () -> Unit,
    onLogoutSuccess: () -> Unit,
    mainViewModel: MainViewModel,
    viewModel: HomeViewModel = hiltViewModel(),
    assistantViewModel: com.example.phoenx.ui.screens.assistant.AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val assistantX by assistantViewModel.bubbleX.collectAsState()
    val assistantY by assistantViewModel.bubbleY.collectAsState()
    val isAssistantChatOpen by assistantViewModel.isChatOpen.collectAsState()

    val daysSincePresence by viewModel.daysSincePresence.collectAsState()
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
    val isVideoBannerDismissed by mainViewModel.isVideoBannerDismissed.collectAsState()
    val pendingInvites by mainViewModel.pendingInvitations.collectAsState()
    val isCreator by mainViewModel.isCreator.collectAsState()
    val myRoles by mainViewModel.myRoles.collectAsState()
    val currentPerspective by mainViewModel.currentPerspective.collectAsState()
    val hasSeenStepByStepNudge by viewModel.hasSeenStepByStepNudge.collectAsState()
    
    // v8.9.0 : Thème Global
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showStepByStepNudge by remember { mutableStateOf(false) } // v9.4.26
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

    if (showStepByStepNudge) {
        AlertDialog(
            onDismissRequest = { 
                showStepByStepNudge = false
                viewModel.markStepByStepNudgeSeen()
            },
            containerColor = theme.backgroundColor,
            title = { Text("Nouvel outil", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Vous pouvez aussi créer votre souvenir étape par étape — appuyez sur la petite flèche pour choisir.", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    showStepByStepNudge = false
                    viewModel.markStepByStepNudgeSeen()
                    onNavigateToCapture(Screen.Capture.TYPE_TEXT, null)
                }) {
                    Text("Compris", color = accent, fontWeight = FontWeight.Bold)
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
                    // --- MON LIVRE (Positionné en haut v9.2.3) ---
                    BookCoverCard(
                        title = uiState.bookTitle ?: "Livre de Vie",
                        chaptersCount = uiState.validatedChaptersCount,
                        coverImageUrl = uiState.coverImageUrl,
                        defaultCoverUrl = uiState.defaultCoverUrl,
                        coverTitleStyle = uiState.coverTitleStyle,
                        scale = uiState.coverScale,
                        offsetX = uiState.coverOffsetX,
                        offsetY = uiState.coverOffsetY,
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
                        var showModeMenu by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1.3f)) {
                            Button(
                                onClick = { 
                                    if (!hasSeenStepByStepNudge) {
                                        showStepByStepNudge = true
                                    } else {
                                        onNavigateToCapture(Screen.Capture.TYPE_TEXT, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
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
                                    
                                    // Petit bouton pour menu (v9.4.26)
                                    IconButton(
                                        onClick = { showModeMenu = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.ArrowDropDown, null, tint = theme.backgroundColor.copy(alpha = 0.6f))
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showModeMenu,
                                onDismissRequest = { showModeMenu = false },
                                containerColor = theme.backgroundColor
                            ) {
                                DropdownMenuItem(
                                    text = { Text("L'Atelier (Rapide)", color = theme.contentColor) },
                                    leadingIcon = { Icon(Icons.Outlined.FlashOn, null, tint = accent) },
                                    onClick = {
                                        showModeMenu = false
                                        viewModel.markStepByStepNudgeSeen()
                                        onNavigateToCapture(Screen.Capture.TYPE_TEXT, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Étape par étape", color = theme.contentColor) },
                                    leadingIcon = { Icon(Icons.Outlined.List, null, tint = accent) },
                                    onClick = {
                                        showModeMenu = false
                                        viewModel.markStepByStepNudgeSeen()
                                        onNavigateToStepByStep()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Appareil Photo", color = theme.contentColor) },
                                    leadingIcon = { Icon(Icons.Outlined.CameraAlt, null, tint = accent) },
                                    onClick = {
                                        showModeMenu = false
                                        onNavigateToCapture(Screen.Capture.TYPE_CAMERA_PHOTO, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Caméra Vidéo", color = theme.contentColor) },
                                    leadingIcon = { Icon(Icons.Outlined.Videocam, null, tint = accent) },
                                    onClick = {
                                        showModeMenu = false
                                        onNavigateToCapture(Screen.Capture.TYPE_CAMERA_VIDEO, null)
                                    }
                                )
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
                                Text("Ma Bibliothèque", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium))
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
                                Text("Mon Fil de Pensée", color = accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium))
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

                    // --- MASQUÉ v9.4.22 : GALERIE DE VIDÉOS DE PRÉSENTATION ---
                    /*
                    PresentationVideoGallery(
                        videos = uiState.presentationVideos,
                        theme = theme,
                        onVideoClick = { video ->
                            selectedPresentationVideo = video
                        }
                    )
                    */

                    // --- NOUVEL EMPLACEMENT v9.4.22 : MON ARBRE GÉNÉALOGIQUE ---
                    GenealogyCard(
                        imageUrl = uiState.genealogyCardImageUrl,
                        onClick = onNavigateToGenealogy,
                        theme = theme,
                        modifier = Modifier.fillMaxWidth()
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

        // v9.4.25 : Bulle Assistant IA
        com.example.phoenx.ui.components.FloatingAssistantBubble(
            initialX = assistantX,
            initialY = assistantY,
            onPositionChanged = { x, y -> assistantViewModel.savePosition(x, y) },
            onClick = { assistantViewModel.toggleChat() }
        )

        if (isAssistantChatOpen) {
            com.example.phoenx.ui.screens.assistant.AssistantChatPanel(
                viewModel = assistantViewModel,
                onDismiss = { assistantViewModel.toggleChat() }
            )
        }
    }
}


