package com.example.phoenx.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.phoenx.domain.model.UserRole
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.auth.AuthScreen
import com.example.phoenx.ui.screens.capture.CaptureScreen
import com.example.phoenx.ui.screens.depositary.*
import com.example.phoenx.ui.screens.detective.DetectiveCreateScreen
import com.example.phoenx.ui.screens.detective.DetectiveHomeScreen
import com.example.phoenx.ui.screens.detective.DetectivePlayerScreen
import com.example.phoenx.ui.screens.fil.FilScreen
import com.example.phoenx.ui.screens.home.HomeScreen
import com.example.phoenx.ui.screens.mappemonde.LocationDetailScreen
import com.example.phoenx.ui.screens.mappemonde.MapMode
import com.example.phoenx.ui.screens.mappemonde.MappamondeScreen
import com.example.phoenx.ui.screens.onboarding.OnboardingScreen
import com.example.phoenx.ui.screens.portrait.EssencePortraitScreen
import com.example.phoenx.ui.screens.legacy.LegacyPreparationScreen
import com.example.phoenx.ui.screens.legacy.UniqueKeyScreen
import com.example.phoenx.ui.screens.pact.PactDetailScreen
import com.example.phoenx.ui.screens.pact.PactScreen
import com.example.phoenx.ui.screens.favorites.FavoritesScreen
import com.example.phoenx.ui.screens.library.*
import com.example.phoenx.ui.screens.recipient.RecipientPermissionsScreen
import com.example.phoenx.ui.screens.mailbox.MailboxScreen
import com.example.phoenx.ui.screens.witness.WitnessInviteScreen
import com.example.phoenx.ui.screens.witness.WitnessResponseScreen
import com.example.phoenx.ui.screens.portraits.PortraitProcheScreen
import com.example.phoenx.ui.screens.portraits.PortraitScreen
import com.example.phoenx.ui.screens.profile.ProfileScreen
import com.example.phoenx.ui.screens.trustcircle.CercleConfianceScreen
import com.example.phoenx.ui.screens.questions.HundredQuestionsScreen
import com.example.phoenx.ui.screens.questions.HundredQuestionsViewModel
import com.example.phoenx.ui.screens.questions.QuestionsScreen
import com.example.phoenx.ui.screens.questions.AskQuestionScreen
import com.example.phoenx.ui.screens.questions.PendingQuestionsScreen
import com.example.phoenx.ui.screens.fil.MemoryDetailScreen
import com.example.phoenx.ui.screens.questionsroom.QuestionsRoomScreen
import com.example.phoenx.ui.screens.quiz.QuizCreateScreen
import com.example.phoenx.ui.screens.quiz.QuizLeaderboardScreen
import com.example.phoenx.ui.screens.quiz.QuizPlayScreen
import com.example.phoenx.ui.screens.universal.UniversalMessageScreen
import com.example.phoenx.ui.screens.universal.UniversalFeedScreen
import com.example.phoenx.ui.screens.universal.UniversalJoinScreen
import com.example.phoenx.ui.screens.universal.GuestDashboardScreen
import com.example.phoenx.ui.screens.recipient.*
import com.example.phoenx.ui.screens.silence.SilenceBlockScreen
import com.example.phoenx.ui.screens.silence.SilenceCheckInScreen
import com.example.phoenx.ui.screens.silence.SilenceOnboardingScreen
import com.example.phoenx.ui.screens.splash.SplashScreen
import com.example.phoenx.service.SilenceStatus
import com.example.phoenx.ui.screens.reconciliation.ReconciliationScreen
import com.example.phoenx.ui.screens.recovery.RecoveryScreen
import com.example.phoenx.ui.screens.worlds.WorldsScreen
import com.example.phoenx.ui.screens.book.BookEditorScreen
import com.example.phoenx.ui.screens.book.BookViewerScreen
import com.example.phoenx.ui.screens.youngselfletters.YoungSelfLetterScreen
import com.example.phoenx.ui.screens.settings.SettingsScreen
import com.example.phoenx.ui.screens.settings.AccessibilitySettingsScreen
import com.example.phoenx.ui.screens.settings.NotificationContactsScreen
import com.example.phoenx.ui.screens.settings.ProtocolSettingsScreen
import com.example.phoenx.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth

@androidx.media3.common.util.UnstableApi
@Composable
fun PhoenXNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    onVerifyBiometrics: (onSuccess: () -> Unit) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onAnimationFinished = {
                val user = FirebaseAuth.getInstance().currentUser
                val destination = if (user == null) Screen.Onboarding.route else Screen.Home.route
                navController.navigate(destination) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen { isSignup ->
                val nextRoute = if (isSignup) Screen.Auth.Signup.createRoute() else Screen.Auth.Login.createRoute()
                navController.navigate(nextRoute)
            }
        }
        
        composable(
            route = Screen.Auth.Signup.route,
            arguments = listOf(navArgument("redirectTo") { nullable = true; type = NavType.StringType })
        ) { backStackEntry ->
            val redirectTo = backStackEntry.arguments?.getString("redirectTo")
            val isGuestFlow = redirectTo?.contains("depositary") == true || redirectTo?.startsWith("join/") == true

            AuthScreen(
                isSignup = true, 
                isGuestFlow = isGuestFlow,
                onAuthSuccess = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        mainViewModel.checkSilenceOnLaunch(uid)
                    }
                    
                    if (redirectTo != null) {
                        // Tenter de revenir à l'écran appelant si redirectTo est défini
                        if (!navController.popBackStack()) {
                            navController.navigate(redirectTo) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                    }
                },
                onNavigateToRecovery = { navController.navigate(Screen.Auth.Recovery.route) }
            )
        }
        
        composable(
            route = Screen.Auth.Login.route,
            arguments = listOf(navArgument("redirectTo") { nullable = true; type = NavType.StringType })
        ) { backStackEntry ->
            val redirectTo = backStackEntry.arguments?.getString("redirectTo")
            val isGuestFlow = redirectTo?.contains("depositary") == true || redirectTo?.startsWith("join/") == true
            
            AuthScreen(
                isSignup = false, 
                isGuestFlow = isGuestFlow,
                onAuthSuccess = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        mainViewModel.checkSilenceOnLaunch(uid)
                    }
                    
                    if (redirectTo != null) {
                        // Tenter de revenir à l'écran appelant
                        if (!navController.popBackStack()) {
                            navController.navigate(redirectTo) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                    }
                },
                onNavigateToRecovery = { navController.navigate(Screen.Auth.Recovery.route) }
            )
        }

        composable(Screen.Home.route) {
            val silenceStatus by mainViewModel.silenceStatus.collectAsState()
            val isSilenceOnboardingDone by mainViewModel.isSilenceOnboardingDone.collectAsState()
            val isCreator by mainViewModel.isCreator.collectAsState()
            val myRoles by mainViewModel.myRoles.collectAsState()
            
            val isLoggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
            val isEmailVerified = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isEmailVerified ?: false

            // Flag pour empêcher les redirections multiples
            var hasNavigated by remember { mutableStateOf(false) }

            if (isLoggedIn && isEmailVerified && isCreator == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1F)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFC97B3A),
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
                return@composable
            }

            LaunchedEffect(isCreator, myRoles, silenceStatus, isSilenceOnboardingDone) {
                if (hasNavigated) return@LaunchedEffect

                if (isCreator == true) {
                    // ROUTAGE CRÉATEUR
                    if (isSilenceOnboardingDone == false) {
                        hasNavigated = true
                        navController.navigate(Screen.SilenceOnboarding.route)
                    } else if (isSilenceOnboardingDone == true) {
                        when (silenceStatus) {
                            SilenceStatus.CHECK_IN_DUE -> {
                                hasNavigated = true
                                navController.navigate(Screen.SilenceCheckIn.route)
                            }
                            SilenceStatus.BLOCKED -> {
                                hasNavigated = true
                                navController.navigate(Screen.SilenceBlock.route)
                            }
                            else -> {}
                        }
                    }
                } else if (isCreator == false) {
                    // ROUTAGE INVITÉ (v7.2)
                    hasNavigated = true
                    if (myRoles.isEmpty()) {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    } else {
                        // Un seul rôle de dépositaire -> On garde l'ancien dashboard direct par confort
                        val rolesList = myRoles.values.toList()
                        if (rolesList.size == 1 && rolesList.first().role == "depositary") {
                            navController.navigate(Screen.DepositaryDashboard.createRoute(rolesList.first().creatorId)) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        } else {
                            // Plusieurs rôles ou rôle différent -> Dashboard unifié
                            navController.navigate("guest_dashboard") {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }
                }
            }

            HomeScreen(
                onNavigateToCapture = { type, prompt -> 
                    navController.navigate(Screen.Capture.createRoute(type, prompt)) 
                },
                onNavigateToFil = { navController.navigate(Screen.Fil.route) },
                onNavigateToLetters = { navController.navigate(Screen.YoungSelfLetters.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToTrustCircle = { navController.navigate(Screen.TrustCircle.route) },
                onNavigateToEssence = { navController.navigate(Screen.Essence.route) },
                onNavigateToPortraits = { navController.navigate(Screen.Portraits.createRoute()) },
                onNavigateToWorlds = { navController.navigate(Screen.Worlds.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToQuestions = { navController.navigate(Screen.Questions.route) },
                onNavigateToPendingQuestions = { navController.navigate(Screen.PendingQuestions.route) },
                onNavigateToMailbox = { navController.navigate(Screen.RecipientMailbox.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.createRoute()) },
                onNavigateToLibrary = { navController.navigate(Screen.RecipientLibrary.route) },
                onNavigateToDetective = { navController.navigate(Screen.DetectiveHome.route) },
                onNavigateToNotificationContacts = { navController.navigate(Screen.NotificationContacts.route) },
                onNavigateToAccessibility = { navController.navigate(Screen.AccessibilitySettings.route) },
                onLogoutSuccess = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                mainViewModel = mainViewModel
            )
        }

        composable(Screen.Fil.route) {
            FilScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Capture.route,
            arguments = listOf(
                navArgument("type") { defaultValue = Screen.Capture.TYPE_TEXT },
                navArgument("prompt") { nullable = true },
                navArgument("pactId") { nullable = true },
                navArgument("pendingQuestionId") { nullable = true },
                navArgument("lat") { nullable = true },
                navArgument("lng") { nullable = true },
                navArgument("locationName") { nullable = true },
                navArgument("locationId") { nullable = true },
                navArgument("parentEntryId") { nullable = true }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: Screen.Capture.TYPE_TEXT
            val prompt = backStackEntry.arguments?.getString("prompt")
            val pactId = backStackEntry.arguments?.getString("pactId")
            val pendingQuestionId = backStackEntry.arguments?.getString("pendingQuestionId")
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
            val locationName = backStackEntry.arguments?.getString("locationName")
            val locationId = backStackEntry.arguments?.getString("locationId")
            val parentEntryId = backStackEntry.arguments?.getString("parentEntryId")

            CaptureScreen(
                initialType = type, 
                initialText = prompt ?: "",
                pactId = pactId,
                pendingQuestionId = pendingQuestionId,
                latitude = lat,
                longitude = lng,
                locationName = locationName,
                locationId = locationId,
                parentEntryId = parentEntryId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> 
                    navController.navigate(Screen.MemoryDetail.createRoute(id)) {
                        // Nettoyage de la pile pour que "Retour" depuis le détail ramène à l'accueil
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        
        composable(Screen.YoungSelfLetters.route) {
            YoungSelfLetterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Map.route,
            arguments = listOf(
                navArgument("returnToEntryId") { nullable = true; type = NavType.StringType }
            )
        ) { backStackEntry ->
            val returnToEntryId = backStackEntry.arguments?.getString("returnToEntryId")
            val mode = if (returnToEntryId != null) MapMode.PICKER else MapMode.CREATOR
            
            MappamondeScreen(
                navController = navController,
                mode = mode,
                returnToEntryId = returnToEntryId
            )
        }

        composable(Screen.MapRecipient.route) {
            MappamondeScreen(
                navController = navController,
                mode = MapMode.RECIPIENT
            )
        }

        composable(
            route = Screen.LocationDetail.route,
            arguments = listOf(navArgument("locationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId") ?: ""
            LocationDetailScreen(
                locationId = locationId,
                navController = navController,
                mode = MapMode.CREATOR
            )
        }

        composable(Screen.Library.route) {
            RecipientLibraryScreen(navController = navController, isCreatorMode = true)
        }

        composable(
            route = "library_cover_picker/{compartmentId}/{compartmentName}",
            arguments = listOf(
                navArgument("compartmentId") { type = NavType.StringType },
                navArgument("compartmentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val compId = backStackEntry.arguments?.getString("compartmentId") ?: ""
            val compName = backStackEntry.arguments?.getString("compartmentName") ?: ""
            LibraryCoverPickerScreen(
                compartmentId = compId,
                compartmentName = compName,
                navController = navController
            )
        }

        composable(Screen.Questions.route) {
            HundredQuestionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAnswerQuestion = { id, text ->
                    navController.navigate(Screen.Capture.createRoute(
                        type = Screen.Capture.TYPE_TEXT,
                        prompt = text
                    ))
                }
            )
        }

        composable("universal_message") {
            UniversalMessageScreen(navController = navController)
        }

        composable("universal_feed") {
            UniversalFeedScreen(navController = navController)
        }

        composable("guest_dashboard") {
            GuestDashboardScreen(
                navController = navController,
                mainViewModel = mainViewModel,
                onLogout = {
                    mainViewModel.logout()
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("questions_room") {
            QuestionsRoomScreen(
                onNavigateBack = { navController.popBackStack() },
                onAnswerQuestion = { id, text ->
                    navController.navigate(com.example.phoenx.ui.navigation.Screen.Capture.createRoute(
                        type = com.example.phoenx.ui.navigation.Screen.Capture.TYPE_TEXT,
                        prompt = text
                    ))
                }
            )
        }

        composable(Screen.Worlds.route) {
            WorldsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWorld = { _ ->
                    // Pour le moment on reste sur l'écran
                }
            )
        }

        composable(
            route = Screen.Portraits.route,
            arguments = listOf(navArgument("recipientId") { nullable = true })
        ) { backStackEntry ->
            val recipientId = backStackEntry.arguments?.getString("recipientId")
            PortraitScreen(
                initialRecipientId = recipientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Pact.route) {
            PactScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate("pact/$id") }
            )
        }

        composable("pact/{pactId}") { backStackEntry ->
            val pactId = backStackEntry.arguments?.getString("pactId") ?: ""
            PactDetailScreen(
                pactId = pactId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { id, name -> 
                    navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_TEXT, "Vers ma vérité avec $name", id))
                }
            )
        }

        composable(Screen.Recipients.route) {
            RecipientScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.RecipientDetail.createRoute(id)) }
            )
        }

        composable(Screen.RecipientDetail.route) { backStackEntry ->
            val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
            RecipientDetailScreen(
                recipientId = recipientId,
                onNavigateBack = { navController.popBackStack() },
                onComposePortrait = { id -> navController.navigate(Screen.Portraits.createRoute(id)) },
                onNavigateToPermissions = { id -> navController.navigate(Screen.RecipientPermissions.createRoute(id)) }
            )
        }

        composable(Screen.RecipientPermissions.route) { backStackEntry ->
            val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
            RecipientPermissionsScreen(
                recipientId = recipientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PendingQuestions.route) {
            PendingQuestionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAnswerQuestion = { questionId ->
                    // Navigation vers la capture pour répondre avec l'ID de la question
                    navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_TEXT, pendingQuestionId = questionId))
                }
            )
        }

        composable(
            route = Screen.MemoryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            MemoryDetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
        }

        composable(
            route = Screen.AskQuestion.route,
            deepLinks = listOf(navDeepLink {
                uriPattern = "https://phoenx.app/ask?creator={creatorId}&recipient={recipientId}"
            })
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
            AskQuestionScreen(
                creatorId = creatorId,
                recipientId = recipientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RecipientCube.route,
            arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            RecipientCubeScreen(
                creatorId = creatorId,
                onExit = { navController.popBackStack() },
                onNavigateToLibrary = { navController.navigate(Screen.RecipientLibrary.createRoute(creatorId)) },
                onNavigateToDiscotheque = { navController.navigate(Screen.RecipientDiscotheque.route) },
                onNavigateToArchives = { navController.navigate(Screen.RecipientFavorites.createRoute(creatorId)) },
                onBecomeCreator = { navController.navigate(Screen.SilenceOnboarding.route) }
            )
        }

        composable(
            route = Screen.RecipientFavorites.route,
            arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            RecipientArchiveScreen(
                creatorId = creatorId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RecipientLibrary.route,
            arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            RecipientLibraryScreen(
                navController = navController, 
                isCreatorMode = false,
                targetCreatorId = creatorId
            )
        }

        // --- ROUTES BIBLIOTHÈQUE ---
        composable("library_books") {
            RecipientBooksScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { navController.navigate("capture/TEXT") }
            )
        }
        composable("library_music") {
            RecipientDiscothequeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { navController.navigate("capture/AUDIO") },
                onNavigateToDetail = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) }
            )
        }
        composable("library_video") {
            RecipientVideothequeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { navController.navigate("capture/VIDEO") },
                onNavigateToDetail = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) }
            )
        }
        composable("fil_pensee") {
            FilScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("lettres") {
            MailboxScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("mes_meilleurs") {
            FavoritesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("photos") {
            RecipientPhotosScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { navController.navigate("capture/PHOTO") },
                onNavigateToDetail = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) }
            )
        }
        composable("mappemonde") {
            MappamondeScreen(navController = navController, mode = MapMode.CREATOR)
        }
        composable("cent_questions") {
            QuestionsRoomScreen(
                onNavigateBack = { navController.popBackStack() },
                onAnswerQuestion = { id, text ->
                    navController.navigate(com.example.phoenx.ui.navigation.Screen.Capture.createRoute(
                        type = com.example.phoenx.ui.navigation.Screen.Capture.TYPE_TEXT,
                        prompt = text
                    ))
                }
            )
        }
        composable("coffre_fort") {
            DetectiveHomeScreen(navController = navController)
        }
        composable("tiroir_secret") {
            UniqueKeyScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("le_pacte") {
            PactScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToDetail = { id -> navController.navigate("pact/$id") })
        }
        composable("portrait_proche") {
            PortraitProcheScreen(
                initialRecipientId = null,
                navController = navController
            )
        }
        composable("reconciliation") {
            ReconciliationScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Côté Créateur
        composable("witness_invite") {
            WitnessInviteScreen(
                navController = navController,
                mainViewModel = mainViewModel
            )
        }

        // Côté Témoin (deeplink depuis email OU accès authentifié v7.2)
        composable(
            route = "witness_response/{creatorId}/{witnessId}/{token}",
            arguments = listOf(
                navArgument("creatorId") { type = NavType.StringType },
                navArgument("witnessId") { type = NavType.StringType },
                navArgument("token") { nullable = true; defaultValue = "none" }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://phoenx.app/witness?creator={creatorId}&witness={witnessId}&token={token}"
                }
            )
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            val witnessId = backStackEntry.arguments?.getString("witnessId") ?: ""
            val token = backStackEntry.arguments?.getString("token")
            val finalToken = if (token == "none") null else token

            WitnessResponseScreen(
                creatorId = creatorId,
                witnessId = witnessId,
                token = finalToken,
                navController = navController
            )
        }

        // --- LIVRE DE VIE ---
        composable("book_editor") {
            BookEditorScreen(navController = navController)
        }
        composable("book_viewer") {
            BookViewerScreen(navController = navController, isRecipientMode = false)
        }
        composable(
            route = "book_viewer_recipient?creatorId={creatorId}",
            arguments = listOf(navArgument("creatorId") { nullable = true })
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId")
            BookViewerScreen(navController = navController, isRecipientMode = true, targetCreatorId = creatorId)
        }

        // --- DEPOSITARY GRAPH ---
        composable(
            route = Screen.DepositaryWelcome.route,
            arguments = listOf(
                navArgument("shortCode") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://phoenx.app/invite/{shortCode}" }
            )
        ) { backStackEntry ->
            val shortCode = backStackEntry.arguments?.getString("shortCode") ?: ""

            val user = FirebaseAuth.getInstance().currentUser
            val onboardingSeen by if (user != null) {
                mainViewModel.isDepositaryOnboardingSeen(user.uid).collectAsState(initial = true)
            } else {
                remember { mutableStateOf(true) }
            }
            
            DepositaryWelcomeScreen(
                shortCode = shortCode,
                onUnderstood = {
                    if (!onboardingSeen) {
                        navController.navigate("depositary_onboarding")
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                },
                onNavigateToAuth = { code ->
                    // On envoie vers le login avec un redirect vers cet écran précis
                    navController.navigate(Screen.Auth.Login.createRoute(Screen.DepositaryWelcome.createRoute(code)))
                }
            )
        }

        composable("depositary_onboarding") {
            val user = FirebaseAuth.getInstance().currentUser
            val firstCreatorId by mainViewModel.firstProtectedCreatorId.collectAsState()
            
            DepositaryOnboardingScreen(
                creatorName = "Ton proche", 
                onFinish = {
                    user?.uid?.let { uid ->
                        mainViewModel.markDepositaryOnboardingSeen(uid)
                        val targetId = firstCreatorId ?: uid // Fallback sur soi-même si pas encore de liaison (cas rare)
                        navController.navigate(Screen.DepositaryDashboard.createRoute(targetId)) {
                            popUpTo("depositary_onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.DepositaryDashboard.route,
            arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            DepositaryDashboardScreen(
                creatorId = creatorId,
                onNavigateToActivation = { id ->
                    navController.navigate(Screen.DepositaryActivation.createRoute(id))
                },
                onNavigateToOnboarding = {
                    navController.navigate("depositary_onboarding")
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.DepositaryNotifications.route)
                },
                onNavigateToInfo = {
                    navController.navigate(Screen.DepositaryInfo.route)
                }
            )
        }

        composable(
            route = Screen.DepositaryActivation.route,
            arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            val viewModel: DepositaryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            DepositaryActivationScreen(
                creatorId = creatorId,
                depositaryId = "primary",
                creatorName = uiState.creatorName,
                onActivationComplete = { navController.navigate(Screen.DepositaryDashboard.createRoute(creatorId)) },
                onCancel = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(
            route = "depositary_alert?level={level}&uid={creatorId}",
            arguments = listOf(
                navArgument("level") { type = NavType.IntType; defaultValue = 3 },
                navArgument("creatorId") { type = NavType.StringType; defaultValue = "" }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://phoenx.app/depositary-alert?level={level}&uid={creatorId}" }
            )
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getInt("level") ?: 3
            val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
            DepositaryAlertReceivedScreen(
                escalationLevel = level,
                creatorId = creatorId,
                navController = navController
            )
        }

        // --- RÉGLAGES ET AUTRES ---
        composable(Screen.Essence.route) {
            EssencePortraitScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToProtocol = { navController.navigate(Screen.ProtocolSettings.route) },
                onNavigateToAccessibility = { navController.navigate(Screen.AccessibilitySettings.route) },
                onNavigateToNotificationContacts = { navController.navigate(Screen.NotificationContacts.route) },
                onNavigateToReconciliation = { navController.navigate(Screen.Reconciliation.route) },
                onNavigateToRecipients = { navController.navigate(Screen.Recipients.route) },
                onNavigateToUniqueKey = { navController.navigate(Screen.UniqueKey.route) },
                onNavigateToDetective = { navController.navigate(Screen.DetectiveHome.route) },
                onVerifyBiometrics = onVerifyBiometrics,
                mainViewModel = mainViewModel
            )
        }

        composable(Screen.UniqueKey.route) {
            UniqueKeyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Reconciliation.route) {
            ReconciliationScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.TrustCircle.route) {
            CercleConfianceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProtocol = { navController.navigate(Screen.ProtocolSettings.route) },
                onNavigateToWitnesses = { navController.navigate(Screen.WitnessInvite.route) },
                onNavigateToRecipients = { navController.navigate(Screen.Recipients.route) },
                onNavigateToNotifications = { navController.navigate(Screen.NotificationContacts.route) }
            )
        }

        composable(
            route = Screen.UniversalJoin.route,
            deepLinks = listOf(navDeepLink { uriPattern = "https://phoenx.app/join/{token}" })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            UniversalJoinScreen(
                token = token,
                onNavigateToAuth = { t ->
                    navController.navigate(Screen.Auth.Login.createRoute(Screen.UniversalJoin.createRoute(t)))
                },
                onSuccess = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        mainViewModel.checkSilenceOnLaunch(uid)
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.UniversalJoin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SilenceOnboarding.route) {
            val scope = rememberCoroutineScope()
            SilenceOnboardingScreen(onConfirmRythm = { days ->
                scope.launch {
                    mainViewModel.setSilenceConfig(days)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SilenceOnboarding.route) { inclusive = true }
                    }
                }
            })
        }

        composable(Screen.SilenceCheckIn.route) {
            SilenceCheckInScreen(
                onImHere = { 
                    mainViewModel.recordCheckIn("present")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SilenceCheckIn.route) { inclusive = true }
                    }
                },
                onTraversingSomething = { action ->
                    mainViewModel.recordCheckIn("traversing")
                    if (action == "record") {
                        navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_NIGHT))
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SilenceCheckIn.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.SilenceBlock.route) {
            val daysMissed by mainViewModel.daysSinceLastCheckIn.collectAsState()
            SilenceBlockScreen(
                daysSinceLastCheckIn = daysMissed,
                onImHere = { 
                    mainViewModel.recordCheckIn("present")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SilenceBlock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProtocolSettings.route) {
            ProtocolSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AccessibilitySettings.route) {
            AccessibilitySettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                mainViewModel = mainViewModel
            )
        }

        composable(Screen.NewLegacy.route) {
            LegacyPreparationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecipients = { navController.navigate(Screen.Recipients.route) }
            )
        }

        composable(Screen.RecipientMailbox.route) {
            MailboxScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.RecipientDetective.route) { backStackEntry ->
            val creatorId = backStackEntry.arguments?.getString("creatorId")
            DetectivePlayerScreen(
                creatorId = creatorId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DetectiveHome.route) {
            DetectiveHomeScreen(navController = navController)
        }

        composable("detective_create") {
            DetectiveCreateScreen(navController = navController)
        }

        composable(Screen.NotificationContacts.route) {
            NotificationContactsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // --- QUIZ ---
        composable("quiz_create") {
            QuizCreateScreen(navController = navController)
        }

        composable(
            route = "quiz_play/{creatorId}/{quizId}",
            arguments = listOf(
                navArgument("creatorId") { type = NavType.StringType },
                navArgument("quizId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            QuizPlayScreen(
                creatorId = backStackEntry.arguments?.getString("creatorId") ?: "",
                quizId = backStackEntry.arguments?.getString("quizId") ?: "",
                navController = navController
            )
        }

        composable(
            route = "quiz_leaderboard/{creatorId}/{quizId}",
            arguments = listOf(
                navArgument("creatorId") { type = NavType.StringType },
                navArgument("quizId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            QuizLeaderboardScreen(
                creatorId = backStackEntry.arguments?.getString("creatorId") ?: "",
                quizId = backStackEntry.arguments?.getString("quizId") ?: "",
                navController = navController
            )
        }

        composable(Screen.DepositaryNotifications.route) {
            DepositaryNotificationsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.DepositaryInfo.route) {
            DepositaryInfoScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                mainViewModel = mainViewModel
            )
        }
    }
}
