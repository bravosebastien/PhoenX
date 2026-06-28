package com.example.phoenx.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.auth.AuthScreen
import com.example.phoenx.ui.screens.capture.CaptureScreen
import com.example.phoenx.ui.screens.depositary.DepositaryScreen
import com.example.phoenx.ui.screens.detective.DetectiveScreen
import com.example.phoenx.ui.screens.fil.FilScreen
import com.example.phoenx.ui.screens.home.HomeScreen
import com.example.phoenx.ui.screens.map.MapScreen
import com.example.phoenx.ui.screens.onboarding.OnboardingScreen
import com.example.phoenx.ui.screens.portrait.EssencePortraitScreen
import com.example.phoenx.ui.screens.legacy.LegacyPreparationScreen
import com.example.phoenx.ui.screens.legacy.UniqueKeyScreen
import com.example.phoenx.ui.screens.pact.PactDetailScreen
import com.example.phoenx.ui.screens.pact.PactScreen
import com.example.phoenx.ui.screens.favorites.FavoritesScreen
import com.example.phoenx.ui.screens.library.LibraryScreen
import com.example.phoenx.ui.screens.library.ViewerMode
import com.example.phoenx.ui.screens.mailbox.MailboxScreen
import com.example.phoenx.ui.screens.portraits.PortraitScreen
import com.example.phoenx.ui.screens.questions.QuestionsScreen
import com.example.phoenx.ui.screens.questionsroom.QuestionsRoomScreen
import com.example.phoenx.ui.screens.recipient.RecipientArchiveScreen
import com.example.phoenx.ui.screens.splash.SplashScreen
import com.example.phoenx.ui.screens.recipient.RecipientCubeScreen
import com.example.phoenx.ui.screens.recipient.RecipientDetailScreen
import com.example.phoenx.ui.screens.recipient.RecipientBooksScreen
import com.example.phoenx.ui.screens.recipient.RecipientDiscothequeScreen
import com.example.phoenx.ui.screens.library.RecipientLibraryScreen
import com.example.phoenx.ui.screens.recipient.RecipientScreen
import com.example.phoenx.ui.screens.recipient.RecipientVideothequeScreen
import com.example.phoenx.ui.screens.reconciliation.ReconciliationScreen
import com.example.phoenx.ui.screens.recovery.RecoveryScreen
import com.example.phoenx.ui.screens.worlds.WorldsScreen
import com.example.phoenx.ui.screens.youngselfletters.YoungSelfLetterScreen
import com.example.phoenx.ui.screens.settings.SettingsScreen
import com.example.phoenx.ui.screens.settings.ProtocolSettingsScreen
import com.example.phoenx.ui.screens.settings.AccessibilitySettingsScreen
import com.example.phoenx.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth

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
                onNavigateToCube = { navController.navigate(Screen.RecipientLibrary.route) },
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
                navArgument("pactId") { nullable = true }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: Screen.Capture.TYPE_TEXT
            val prompt = backStackEntry.arguments?.getString("prompt")
            val pactId = backStackEntry.arguments?.getString("pactId")
            CaptureScreen(
                initialType = type, 
                initialText = prompt ?: "",
                pactId = pactId,
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
            MapScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                navController = navController,
                creatorId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                viewerMode = ViewerMode.CREATOR_PREVIEW
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
                onNavigateToWorld = { category ->
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
            RecipientBooksScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("library_music") {
            RecipientDiscothequeScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("library_video") {
            RecipientVideothequeScreen(onNavigateBack = { navController.popBackStack() })
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
            MapScreen(onNavigateBack = { navController.popBackStack() })
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
        // -----------------------------------


        composable(Screen.RecipientDiscotheque.route) {
            RecipientDiscothequeScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.RecipientVideotheque.route) {
            RecipientVideothequeScreen(onNavigateBack = { navController.popBackStack() })
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
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name, color = TextPrimary, style = MaterialTheme.typography.displayMedium)
    }
}
