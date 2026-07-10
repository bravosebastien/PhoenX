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
import com.example.phoenx.ui.screens.questions.HundredQuestionsScreen
import com.example.phoenx.ui.screens.questions.HundredQuestionsViewModel
import com.example.phoenx.ui.screens.questions.QuestionsScreen
import com.example.phoenx.ui.screens.questions.AskQuestionScreen
import com.example.phoenx.ui.screens.questions.PendingQuestionsScreen
import com.example.phoenx.ui.screens.questionsroom.QuestionsRoomScreen
import com.example.phoenx.ui.screens.quiz.QuizCreateScreen
import com.example.phoenx.ui.screens.quiz.QuizLeaderboardScreen
import com.example.phoenx.ui.screens.quiz.QuizPlayScreen
import com.example.phoenx.ui.screens.universal.UniversalMessageScreen
import com.example.phoenx.ui.screens.universal.UniversalFeedScreen
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
                val nextRoute = if (isSignup) Screen.Auth.Signup.route else Screen.Auth.Login.route
                navController.navigate(nextRoute)
            }
        }
        
        composable(Screen.Auth.Signup.route) {
            AuthScreen(
                isSignup = true, 
                onAuthSuccess = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        mainViewModel.checkSilenceOnLaunch(uid)
                    }
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                },
                onNavigateToRecovery = { navController.navigate(Screen.Auth.Recovery.route) }
            )
        }
        
        composable(Screen.Auth.Login.route) {
            AuthScreen(
                isSignup = false, 
                onAuthSuccess = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        mainViewModel.checkSilenceOnLaunch(uid)
                    }
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                },
                onNavigateToRecovery = { navController.navigate(Screen.Auth.Recovery.route) }
            )
        }

        composable(Screen.Home.route) {
            val silenceStatus by mainViewModel.silenceStatus.collectAsState()
            val isSilenceOnboardingDone by mainViewModel.isSilenceOnboardingDone.collectAsState()
            val isLoggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
            val isEmailVerified = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isEmailVerified ?: false

            if (isLoggedIn && isEmailVerified && (silenceStatus == null || isSilenceOnboardingDone == null)) {
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

            LaunchedEffect(silenceStatus, isSilenceOnboardingDone) {
                if (isSilenceOnboardingDone == false) {
                    navController.navigate(Screen.SilenceOnboarding.route)
                } else if (isSilenceOnboardingDone == true) {
                    when (silenceStatus) {
                        SilenceStatus.CHECK_IN_DUE -> navController.navigate(Screen.SilenceCheckIn.route)
                        SilenceStatus.BLOCKED -> navController.navigate(Screen.SilenceBlock.route)
                        else -> {}
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
                onNavigateToEssence = { navController.navigate(Screen.Essence.route) },
                onNavigateToPortraits = { navController.navigate(Screen.Portraits.route) },
                onNavigateToWorlds = { navController.navigate(Screen.Worlds.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToQuestions = { navController.navigate(Screen.Questions.route) },
                onNavigateToPendingQuestions = { navController.navigate(Screen.PendingQuestions.route) },
                onNavigateToMailbox = { navController.navigate(Screen.RecipientMailbox.route) },
                onNavigateToLegacy = { navController.navigate(Screen.NewLegacy.route) },
                onNavigateToRecipients = { navController.navigate(Screen.Recipients.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToLibrary = { navController.navigate(Screen.RecipientLibrary.route) },
                onNavigateToDetective = { navController.navigate(Screen.DetectiveHome.route) },
                onNavigateToNotificationContacts = { navController.navigate(Screen.NotificationContacts.route) },
                onNavigateToAccessibility = { navController.navigate(Screen.AccessibilitySettings.route) },
                mainViewModel = mainViewModel
            )
        }

        composable(Screen.Fil.route) {
            FilScreen(onNavigateBack = { navController.popBackStack() })
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
                navArgument("locationId") { nullable = true }
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

            CaptureScreen(
                initialType = type, 
                initialText = prompt ?: "",
                pactId = pactId,
                pendingQuestionId = pendingQuestionId,
                latitude = lat,
                longitude = lng,
                locationName = locationName,
                locationId = locationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.YoungSelfLetters.route) {
            YoungSelfLetterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Map.route) {
            MappamondeScreen(
                navController = navController,
                mode = MapMode.CREATOR
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

        composable(Screen.RecipientCube.route) {
            RecipientCubeScreen(
                onExit = { navController.popBackStack() },
                onNavigateToLibrary = { navController.navigate(Screen.RecipientLibrary.route) },
                onNavigateToDiscotheque = { navController.navigate(Screen.RecipientDiscotheque.route) },
                onNavigateToArchives = { navController.navigate(Screen.RecipientFavorites.route) }
            )
        }

        composable(Screen.RecipientFavorites.route) {
            RecipientArchiveScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.RecipientLibrary.route) {
            RecipientLibraryScreen(navController = navController)
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
                onNavigateToCapture = { navController.navigate("capture/AUDIO") }
            )
        }
        composable("library_video") {
            RecipientVideothequeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { navController.navigate("capture/VIDEO") }
            )
        }
        composable("fil_pensee") {
            FilScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("lettres") {
            MailboxScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("mes_meilleurs") {
            FavoritesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("photos") {
            RecipientArchiveScreen(onNavigateBack = { navController.popBackStack() })
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
            WitnessInviteScreen(navController = navController)
        }

        // Côté Témoin (deeplink depuis email)
        composable(
            route = "witness_response/{creatorId}/{witnessId}/{token}",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://phoenx.app/witness?creator={creatorId}&witness={witnessId}&token={token}"
                }
            )
        ) { backStackEntry ->
            WitnessResponseScreen(
                creatorId = backStackEntry.arguments?.getString("creatorId") ?: "",
                witnessId = backStackEntry.arguments?.getString("witnessId") ?: "",
                token = backStackEntry.arguments?.getString("token") ?: "",
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
        composable("book_viewer_recipient") {
            BookViewerScreen(navController = navController, isRecipientMode = true)
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
                }
            )
        }

        composable("depositary_onboarding") {
            val user = FirebaseAuth.getInstance().currentUser
            DepositaryOnboardingScreen(
                creatorName = "Ton proche", 
                onFinish = {
                    user?.uid?.let { uid ->
                        mainViewModel.markDepositaryOnboardingSeen(uid)
                        navController.navigate(Screen.DepositaryDashboard.createRoute(uid)) {
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
    }
}
