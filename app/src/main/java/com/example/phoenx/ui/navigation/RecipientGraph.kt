package com.example.phoenx.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.book.BookReaderFlowScreen
import com.example.phoenx.ui.screens.depositary.*
import com.example.phoenx.ui.screens.detective.DetectivePlayerScreen
import com.example.phoenx.ui.screens.fil.MemoryDetailScreen
import com.example.phoenx.ui.screens.library.RecipientLibraryScreen
import com.example.phoenx.ui.screens.mappemonde.MapMode
import com.example.phoenx.ui.screens.mappemonde.MappamondeScreen
import com.example.phoenx.ui.screens.media.MediaViewerScreen
import com.example.phoenx.ui.screens.questions.AskQuestionScreen
import com.example.phoenx.ui.screens.quiz.QuizLeaderboardScreen
import com.example.phoenx.ui.screens.quiz.QuizPlayScreen
import com.example.phoenx.ui.screens.recipient.*
import com.example.phoenx.ui.screens.silence.SilenceBlockScreen
import com.example.phoenx.ui.screens.silence.SilenceCheckInScreen
import com.example.phoenx.ui.screens.silence.SilenceOnboardingScreen
import com.example.phoenx.ui.screens.universal.GuestDashboardScreen
import com.example.phoenx.ui.screens.universal.UniversalFeedScreen
import com.example.phoenx.ui.screens.universal.UniversalJoinScreen
import com.example.phoenx.ui.screens.universal.UniversalMessageScreen
import com.example.phoenx.ui.screens.witness.WitnessResponseScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

fun NavGraphBuilder.recipientGraph(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    composable(Screen.RecipientMailbox.route) {
        com.example.phoenx.ui.screens.mailbox.MailboxScreen(onNavigateBack = { navController.popBackStack() })
    }

    composable(Screen.RecipientDetective.route) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId")
        DetectivePlayerScreen(
            creatorId = creatorId,
            onNavigateBack = { navController.popBackStack() }
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
        val isCreator by mainViewModel.isCreator.collectAsState()
        
        RecipientCubeScreen(
            creatorId = creatorId,
            onExit = { navController.popBackStack() },
            onNavigateToHeritage = { navController.navigate(Screen.HeirHeritage.createRoute(creatorId)) },
            isUserCreator = isCreator ?: true,
            onBecomeCreator = { navController.navigate(Screen.SilenceOnboarding.route) }
        )
    }

    composable(
        route = Screen.HeirHeritage.route,
        arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
    ) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
        HeirHeritageScreen(
            creatorId = creatorId,
            navController = navController
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

    // --- MÉDIATHÈQUE ---
    composable(
        route = Screen.RecipientDiscotheque.route,
        arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
    ) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
        RecipientDiscothequeScreen(
            creatorId = creatorId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCapture = { navController.navigate("capture/AUDIO") },
            onNavigateToDetail = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) }
        )
    }

    composable(
        route = Screen.RecipientVideotheque.route,
        arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
    ) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
        RecipientVideothequeScreen(
            creatorId = creatorId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCapture = { navController.navigate("capture/VIDEO") },
            onNavigateToDetail = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) }
        )
    }

    composable(
        route = Screen.RecipientPhotos.route,
        arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
    ) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
        RecipientPhotosScreen(
            creatorId = creatorId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCapture = { navController.navigate("capture/PHOTO") },
            onNavigateToDetail = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) }
        )
    }

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

    composable(
        route = "book_viewer_recipient?creatorId={creatorId}",
        arguments = listOf(navArgument("creatorId") { nullable = true }),
        deepLinks = listOf(
            navDeepLink { uriPattern = "https://phoenx.app/book/{creatorId}" }
        )
    ) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId")
        BookReaderFlowScreen(navController = navController, targetCreatorId = creatorId)
    }

    // --- DEPOSITARY ---
    composable(
        route = Screen.DepositaryWelcome.route,
        arguments = listOf(navArgument("shortCode") { type = NavType.StringType }),
        deepLinks = listOf(navDeepLink { uriPattern = "https://phoenx.app/invite/{shortCode}" })
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
                if (!onboardingSeen) navController.navigate("depositary_onboarding")
                else navController.navigate(Screen.Home.route)
            },
            onNavigateToAuth = { code ->
                navController.navigate(Screen.Auth.Signup.createRoute(Screen.DepositaryWelcome.createRoute(code)))
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
                    val targetId = firstCreatorId ?: uid
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
            onNavigateToActivation = { id -> navController.navigate(Screen.DepositaryActivation.createRoute(id)) },
            onNavigateToOnboarding = { navController.navigate("depositary_onboarding") },
            onNavigateToNotifications = { navController.navigate(Screen.DepositaryNotifications.route) },
            onNavigateToInfo = { navController.navigate(Screen.DepositaryInfo.route) }
        )
    }

    composable(
        route = Screen.DepositaryActivation.route,
        arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
    ) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
        val viewModel: DepositaryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
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
        deepLinks = listOf(navDeepLink { uriPattern = "https://phoenx.app/depositary-alert?level={level}&uid={creatorId}" })
    ) { backStackEntry ->
        val level = backStackEntry.arguments?.getInt("level") ?: 3
        val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
        DepositaryAlertReceivedScreen(
            escalationLevel = level,
            creatorId = creatorId,
            navController = navController
        )
    }

    // --- SILENCE ---
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
                if (action == "record") navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_NIGHT))
                else navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.SilenceCheckIn.route) { inclusive = true }
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

    // --- UNIVERSAL ---
    composable(
        route = Screen.UniversalJoin.route,
        deepLinks = listOf(navDeepLink { uriPattern = "https://phoenx.app/join/{token}" })
    ) { backStackEntry ->
        val token = backStackEntry.arguments?.getString("token") ?: ""
        UniversalJoinScreen(
            token = token,
            onNavigateToAuth = { t ->
                navController.navigate(Screen.Auth.Signup.createRoute(Screen.UniversalJoin.createRoute(t)))
            },
            onSuccess = {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) mainViewModel.checkSilenceOnLaunch(uid)
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.UniversalJoin.route) { inclusive = true }
                }
            }
        )
    }

    composable("guest_dashboard") {
        GuestDashboardScreen(
            navController = navController,
            mainViewModel = mainViewModel,
            onLogout = {
                mainViewModel.logout()
                navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } }
            }
        )
    }

    composable("universal_message") { UniversalMessageScreen(navController = navController) }
    composable("universal_feed") { UniversalFeedScreen(navController = navController) }

    composable(Screen.DepositaryNotifications.route) { DepositaryNotificationsScreen(onNavigateBack = { navController.popBackStack() }) }
    composable(Screen.DepositaryInfo.route) {
        DepositaryInfoScreen(
            onNavigateBack = { navController.popBackStack() },
            onLogoutSuccess = { navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } } },
            mainViewModel = mainViewModel
        )
    }

    composable(
        route = Screen.MemoryDetail.route,
        arguments = listOf(
            navArgument("entryId") { type = NavType.StringType },
            navArgument("creatorId") { nullable = true; type = NavType.StringType }
        )
    ) { backStackEntry ->
        val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
        val creatorId = backStackEntry.arguments?.getString("creatorId")
        MemoryDetailScreen(
            entryId = entryId,
            onNavigateBack = { navController.popBackStack() },
            navController = navController,
            targetCreatorId = creatorId
        )
    }

    composable(
        route = Screen.MediaViewer.route,
        arguments = listOf(
            navArgument("entryId") { type = NavType.StringType },
            navArgument("creatorId") { nullable = true; type = NavType.StringType }
        )
    ) { backStackEntry ->
        val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
        val creatorId = backStackEntry.arguments?.getString("creatorId")
        MediaViewerScreen(
            entryId = entryId,
            creatorId = creatorId,
            onExit = { navController.popBackStack() }
        )
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
}
