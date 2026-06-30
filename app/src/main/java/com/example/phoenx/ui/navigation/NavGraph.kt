package com.example.phoenx.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.auth.AuthScreen
import com.example.phoenx.ui.screens.capture.CaptureScreen
import com.example.phoenx.ui.screens.depositary.*
import com.example.phoenx.ui.screens.detective.DetectiveScreen
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
import com.example.phoenx.ui.screens.library.RecipientLibraryScreen
import com.example.phoenx.ui.screens.library.LibraryCoverPickerScreen
import com.example.phoenx.ui.screens.mailbox.MailboxScreen
import com.example.phoenx.ui.screens.portraits.PortraitScreen
import com.example.phoenx.ui.screens.questions.QuestionsScreen
import com.example.phoenx.ui.screens.questionsroom.QuestionsRoomScreen
import com.example.phoenx.ui.screens.recipient.*
import com.example.phoenx.ui.screens.silence.SilenceBlockScreen
import com.example.phoenx.ui.screens.silence.SilenceCheckInScreen
import com.example.phoenx.ui.screens.silence.SilenceOnboardingScreen
import com.example.phoenx.ui.screens.splash.SplashScreen
import com.example.phoenx.domain.manager.SilenceStatus
import com.example.phoenx.ui.screens.reconciliation.ReconciliationScreen
import com.example.phoenx.ui.screens.recovery.RecoveryScreen
import com.example.phoenx.ui.screens.witness.WitnessInviteScreen
import com.example.phoenx.ui.screens.worlds.WorldsScreen
import com.example.phoenx.ui.screens.book.BookEditorScreen
import com.example.phoenx.ui.screens.book.BookViewerScreen
import com.example.phoenx.ui.screens.youngselfletters.YoungSelfLetterScreen
import com.example.phoenx.ui.screens.settings.SettingsScreen
import com.example.phoenx.ui.screens.settings.ProtocolSettingsScreen
import com.example.phoenx.ui.screens.settings.AccessibilitySettingsScreen
import com.example.phoenx.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth

@androidx.media3.common.util.UnstableApi
@Composable
fun PhoenXNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onAnimationFinished = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            })
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen { isSignup ->
                if (isSignup) navController.navigate(Screen.Auth.Signup.route)
                else navController.navigate(Screen.Auth.Login.route)
            }
        }
        
        composable(Screen.Auth.Signup.route) {
            AuthScreen(
                isSignup = true, 
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                },
                onNavigateToRecovery = { navController.navigate(Screen.Auth.Recovery.route) }
            )
        }
        
        composable(Screen.Auth.Login.route) {
            AuthScreen(
                isSignup = false, 
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                },
                onNavigateToRecovery = { navController.navigate(Screen.Auth.Recovery.route) }
            )
        }

        composable(Screen.Auth.Recovery.route) {
            RecoveryScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { 
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Auth.Login.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.Home.route) {
            val silenceStatus by mainViewModel.silenceStatus.collectAsState()

            LaunchedEffect(silenceStatus) {
                when (silenceStatus) {
                    SilenceStatus.CHECK_IN_DUE -> navController.navigate(Screen.SilenceCheckIn.route)
                    SilenceStatus.BLOCKED, SilenceStatus.NOTIFY_DEPOSITARY -> navController.navigate(Screen.SilenceBlock.route)
                    else -> {}
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
                onNavigateToMailbox = { navController.navigate(Screen.RecipientMailbox.route) },
                onNavigateToLegacy = { navController.navigate(Screen.NewLegacy.route) },
                onNavigateToRecipients = { navController.navigate(Screen.Recipients.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToLibrary = { navController.navigate(Screen.RecipientLibrary.route) },
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
                navArgument("lat") { nullable = true },
                navArgument("lng") { nullable = true },
                navArgument("locationName") { nullable = true }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: Screen.Capture.TYPE_TEXT
            val prompt = backStackEntry.arguments?.getString("prompt")
            val pactId = backStackEntry.arguments?.getString("pactId")
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
            val locationName = backStackEntry.arguments?.getString("locationName")

            CaptureScreen(
                initialType = type, 
                initialText = prompt ?: "",
                pactId = pactId,
                latitude = lat,
                longitude = lng,
                locationName = locationName,
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
            arguments = listOf(navArgument("locationId") { type = androidx.navigation.NavType.StringType })
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
                navArgument("compartmentId") { type = androidx.navigation.NavType.StringType },
                navArgument("compartmentName") { type = androidx.navigation.NavType.StringType }
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
            QuestionsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("questions_room") {
            QuestionsRoomScreen(onNavigateBack = { navController.popBackStack() })
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

        composable(Screen.Recipients.route) { // New route for circle management
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
                onComposePortrait = { id -> navController.navigate(Screen.Portraits.createRoute(id)) }
            )
        }

        composable(Screen.RecipientCube.route) {
            RecipientCubeScreen(
                onExit = { navController.popBackStack() },
                onNavigateToLibrary = { navController.navigate(Screen.RecipientLibrary.route) },
                onNavigateToDiscotheque = { navController.navigate(Screen.RecipientDiscotheque.route) },
                onNavigateToArchives = { navController.navigate(Screen.RecipientFavorites.route) } // Use favorites route for archives for now if needed, or better:
            )
        }

        composable(Screen.RecipientFavorites.route) { // We'll use this for Archive screen for now or create a specific one
            RecipientArchiveScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.RecipientLibrary.route) {
            RecipientLibraryScreen(navController = navController)
        }

        // --- Routes for the Library Grid ---
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
            MappamondeScreen(
                navController = navController,
                mode = MapMode.CREATOR
            )
        }
        composable("cent_questions") {
            QuestionsRoomScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("coffre_fort") {
            DetectiveScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("tiroir_secret") {
            UniqueKeyScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("le_pacte") {
            PactScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate("pact/$id") }
            )
        }
        composable("portrait_proche") {
            PortraitScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("reconciliation") {
            ReconciliationScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable("book_editor") {
            BookEditorScreen(navController = navController)
        }

        composable("book_viewer") {
            BookViewerScreen(
                navController = navController,
                isRecipientMode = false
            )
        }

        composable("book_viewer_recipient") {
            BookViewerScreen(
                navController = navController,
                isRecipientMode = true
            )
        }
        // -----------------------------------


        composable(Screen.RecipientDiscotheque.route) {
            RecipientDiscothequeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { navController.navigate("capture/AUDIO") }
            )
        }

        composable(Screen.RecipientVideotheque.route) {
            RecipientVideothequeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { navController.navigate("capture/VIDEO") }
            )
        }

        composable(Screen.Essence.route) {
            EssencePortraitScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProtocol = { navController.navigate(Screen.ProtocolSettings.route) },
                onNavigateToAccessibility = { navController.navigate(Screen.AccessibilitySettings.route) },
                onNavigateToReconciliation = { navController.navigate(Screen.Reconciliation.route) },
                onNavigateToRecipients = { navController.navigate(Screen.Recipients.route) },
                onNavigateToUniqueKey = { navController.navigate(Screen.UniqueKey.route) },
                onNavigateToDetective = { navController.navigate(Screen.RecipientDetective.route) },
                mainViewModel = mainViewModel
            )
        }

        composable(Screen.UniqueKey.route) {
            UniqueKeyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Reconciliation.route) {
            ReconciliationScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("witness_invite") {
            WitnessInviteScreen(onNavigateBack = { navController.popBackStack() })
        }

        // --- SILENCE & PREUVE DE VIE ---
        composable(Screen.SilenceOnboarding.route) {
            SilenceOnboardingScreen(onConfirmRythm = { days ->
                mainViewModel.setSilenceConfig(days)
                navController.popBackStack()
            })
        }

        composable(Screen.SilenceCheckIn.route) {
            SilenceCheckInScreen(
                onImHere = { mainViewModel.recordCheckIn("present") },
                onTraversingSomething = { /* BottomSheet logic or navigation */ }
            )
        }

        composable(Screen.SilenceBlock.route) {
            val daysMissed by mainViewModel.daysSinceLastCheckIn.collectAsState()
            SilenceBlockScreen(
                daysSinceLastCheckIn = daysMissed,
                onImHere = { mainViewModel.recordCheckIn("present") }
            )
        }
        // -------------------------------

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

        composable(Screen.RecipientDetective.route) {
            DetectiveScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Depositary.route) {
            DepositaryScreen(
                onConfirm = { /* Logic */ },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.DepositaryWelcome.route) {
            DepositaryWelcomeScreen(onUnderstood = {
                navController.navigate(Screen.DepositaryDashboard.route)
            })
        }

        composable(Screen.DepositaryDashboard.route) {
            DepositaryDashboardScreen(onNavigateToActivation = {
                navController.navigate(Screen.DepositaryActivation.route)
            })
        }

        composable(Screen.DepositaryActivation.route) {
            val viewModel: DepositaryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            DepositaryActivationScreen(
                creatorName = uiState.creatorName,
                onActivationComplete = { navController.navigate(Screen.DepositaryDashboard.route) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.DepositaryNotifications.route) {
            DepositaryNotificationsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}


