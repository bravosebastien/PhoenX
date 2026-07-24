package com.example.phoenx.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.phoenx.service.SilenceStatus
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.book.BookEditorScreen
import com.example.phoenx.ui.screens.book.BookReaderFlowScreen
import com.example.phoenx.ui.screens.capture.CaptureScreen
import com.example.phoenx.ui.screens.detective.DetectiveCreateScreen
import com.example.phoenx.ui.screens.detective.DetectiveHomeScreen
import com.example.phoenx.ui.screens.favorites.FavoritesScreen
import com.example.phoenx.ui.screens.fil.FilScreen
import com.example.phoenx.ui.screens.fil.MemoryDetailScreen
import com.example.phoenx.ui.screens.home.HomeScreen
import com.example.phoenx.ui.screens.legacy.UniqueKeyScreen
import com.example.phoenx.ui.screens.library.LibraryCoverPickerScreen
import com.example.phoenx.ui.screens.library.RecipientLibraryScreen
import com.example.phoenx.ui.screens.mappemonde.LocationDetailScreen
import com.example.phoenx.ui.screens.mappemonde.MapMode
import com.example.phoenx.ui.screens.mappemonde.MappamondeScreen
import com.example.phoenx.ui.screens.pact.PactDetailScreen
import com.example.phoenx.ui.screens.pact.PactScreen
import com.example.phoenx.ui.screens.portraits.PortraitProcheScreen
import com.example.phoenx.ui.screens.portraits.PortraitScreen
import com.example.phoenx.ui.screens.profile.ProfileScreen
import com.example.phoenx.ui.screens.questions.PendingQuestionsScreen
import com.example.phoenx.ui.screens.questions.QuestionsScreen
import com.example.phoenx.ui.screens.quiz.QuizCreateScreen
import com.example.phoenx.ui.screens.reconciliation.ReconciliationScreen
import com.example.phoenx.ui.screens.recipient.RecipientDetailScreen
import com.example.phoenx.ui.screens.recipient.RecipientPermissionsScreen
import com.example.phoenx.ui.screens.recipient.RecipientScreen
import com.example.phoenx.ui.screens.settings.AccessibilitySettingsScreen
import com.example.phoenx.ui.screens.settings.NotificationContactsScreen
import com.example.phoenx.ui.screens.settings.ProtocolSettingsScreen
import com.example.phoenx.ui.screens.settings.SettingsScreen
import com.example.phoenx.ui.screens.trustcircle.CercleConfianceScreen
import com.example.phoenx.ui.screens.witness.WitnessInviteScreen
import com.example.phoenx.ui.screens.worlds.WorldsScreen
import com.example.phoenx.ui.screens.youngselfletters.YoungSelfLetterScreen
import com.google.firebase.auth.FirebaseAuth

fun NavGraphBuilder.creatorGraph(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    composable(Screen.Home.route) {
        val silenceStatus by mainViewModel.silenceStatus.collectAsState()
        val isSilenceOnboardingDone by mainViewModel.isSilenceOnboardingDone.collectAsState()
        val isCreator by mainViewModel.isCreator.collectAsState()
        val myRoles by mainViewModel.myRoles.collectAsState()
        
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        val isEmailVerified = FirebaseAuth.getInstance().currentUser?.isEmailVerified ?: false

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
            } else if (isCreator == false && myRoles.isNotEmpty()) {
                hasNavigated = true
                val rolesList = myRoles.values.toList()
                if (rolesList.size == 1 && rolesList.first().role == "depositary") {
                    navController.navigate(Screen.DepositaryDashboard.createRoute(rolesList.first().creatorId)) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                } else {
                    navController.navigate("guest_dashboard") {
                        popUpTo(Screen.Home.route) { inclusive = true }
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
            onNavigateToIA = { navController.navigate(Screen.Essence.route) },
            onNavigateToPortraits = { navController.navigate(Screen.Portraits.createRoute()) },
            onNavigateToWorlds = { navController.navigate(Screen.Worlds.route) },
            onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
            onNavigateToQuestions = { navController.navigate(Screen.Questions.route) },
            onNavigateToPendingQuestions = { navController.navigate(Screen.PendingQuestions.route) },
            onNavigateToMailbox = { navController.navigate(Screen.RecipientMailbox.route) },
            onNavigateToMap = { navController.navigate(Screen.Map.createRoute()) },
            onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
            onNavigateToDetective = { navController.navigate(Screen.DetectiveHome.route) },
            onNavigateToNotificationContacts = { navController.navigate(Screen.NotificationContacts.route) },
            onNavigateToAccessibility = { navController.navigate(Screen.AccessibilitySettings.route) },
            onNavigateToCube = { id -> 
                val role = myRoles.values.find { it.creatorId == id }
                if (role?.role == "depositary") {
                    navController.navigate(Screen.DepositaryDashboard.createRoute(id))
                } else if (role?.role == "witness") {
                    navController.navigate("witness_response/$id/${role.sourceId}/none")
                } else {
                    navController.navigate(Screen.RecipientCube.createRoute(id))
                }
            },
            onAcceptInvite = { token -> 
                navController.navigate(Screen.UniversalJoin.createRoute(token))
            },
            onBecomeCreator = {
                navController.navigate(Screen.SilenceOnboarding.route)
            },
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
            onNavigateToDetail = { id: String -> 
                navController.navigate(Screen.MemoryDetail.createRoute(id)) {
                    popUpTo(Screen.Home.route)
                }
            }
        )
    }
    
    composable(Screen.YoungSelfLetters.route) {
        YoungSelfLetterScreen(
            navController = navController,
            onNavigateBack = { navController.popBackStack() }
        )
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
        QuestionsScreen(onNavigateBack = { navController.popBackStack() })
    }

    composable("questions_room") {
        QuestionsScreen(onNavigateBack = { navController.popBackStack() })
    }

    composable(Screen.Worlds.route) {
        WorldsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToWorld = { _: String -> }
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
            onNavigateToPermissions = { id -> navController.navigate(Screen.RecipientPermissions.createRoute(id)) },
            navController = navController
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
                navController.navigate(Screen.Capture.createRoute(Screen.Capture.TYPE_TEXT, pendingQuestionId = questionId))
            }
        )
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
            onVerifyBiometrics = { /* Residue removed in v8.9.8 */ },
            mainViewModel = mainViewModel
        )
    }

    composable(Screen.ProtocolSettings.route) {
        ProtocolSettingsScreen(onNavigateBack = { navController.popBackStack() })
    }

    composable(Screen.NotificationContacts.route) {
        NotificationContactsScreen(onNavigateBack = { navController.popBackStack() })
    }

    composable(Screen.AccessibilitySettings.route) {
        AccessibilitySettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            mainViewModel = mainViewModel
        )
    }

    composable("witness_invite") {
        WitnessInviteScreen(
            navController = navController,
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

    composable(Screen.DetectiveHome.route) {
        DetectiveHomeScreen(navController = navController)
    }

    composable("detective_create") {
        DetectiveCreateScreen(navController = navController)
    }

    composable("book_editor") {
        BookEditorScreen(navController = navController)
    }

    composable("book_viewer") {
        BookReaderFlowScreen(navController = navController)
    }

    // --- ALIAS DE ROUTES POUR COMPATIBILITÉ (v8.9.9) ---
    composable("quiz_create") { QuizCreateScreen(navController = navController) }
    composable(
        route = "fil_pensee?creatorId={creatorId}",
        arguments = listOf(navArgument("creatorId") { nullable = true })
    ) { backStackEntry ->
        val creatorId = backStackEntry.arguments?.getString("creatorId")
        FilScreen(
            navController = navController, 
            onNavigateBack = { navController.popBackStack() },
            targetCreatorId = creatorId
        )
    }
    composable("fil_pensee") { 
        FilScreen(navController = navController, onNavigateBack = { navController.popBackStack() }) 
    }
    composable("coffre_fort") { DetectiveHomeScreen(navController = navController) }
    composable("cent_questions") { QuestionsScreen(onNavigateBack = { navController.popBackStack() }) }
    composable("portrait_proche") { PortraitScreen(onNavigateBack = { navController.popBackStack() }) }
    composable("le_pacte") { PactScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToDetail = { id -> navController.navigate("pact/$id") }) }
    composable("lettres") { com.example.phoenx.ui.screens.mailbox.MailboxScreen(onNavigateBack = { navController.popBackStack() }) }
    composable("mappemonde") { MappamondeScreen(navController = navController, mode = MapMode.CREATOR) }
}
