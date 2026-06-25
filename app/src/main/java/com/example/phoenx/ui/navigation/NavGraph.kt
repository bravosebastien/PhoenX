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
import com.example.phoenx.ui.screens.fil.FilScreen
import com.example.phoenx.ui.screens.home.HomeScreen
import com.example.phoenx.ui.screens.onboarding.OnboardingScreen
import com.example.phoenx.ui.screens.portrait.EssencePortraitScreen
import com.example.phoenx.ui.screens.pact.PactScreen
import com.example.phoenx.ui.screens.favorites.FavoritesScreen
import com.example.phoenx.ui.screens.portraits.PortraitScreen
import com.example.phoenx.ui.screens.questions.QuestionsScreen
import com.example.phoenx.ui.screens.reconciliation.ReconciliationScreen
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
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startScreen = if (currentUser != null) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startScreen
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = { isSignup ->
                    if (isSignup) navController.navigate(Screen.Auth.Signup.route)
                    else navController.navigate(Screen.Auth.Login.route)
                }
            )
        }
        
        composable(Screen.Auth.Signup.route) {
            AuthScreen(isSignup = true, onAuthSuccess = {
                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
            })
        }
        
        composable(Screen.Auth.Login.route) {
            AuthScreen(isSignup = false, onAuthSuccess = {
                navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
            })
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
                onNavigateToQuestions = { navController.navigate(Screen.Questions.route) }
            )
        }

        composable(Screen.Fil.route) {
            FilScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(
            route = Screen.Capture.route,
            arguments = listOf(
                navArgument("type") { defaultValue = Screen.Capture.TYPE_TEXT },
                navArgument("prompt") { nullable = true }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: Screen.Capture.TYPE_TEXT
            val prompt = backStackEntry.arguments?.getString("prompt")
            CaptureScreen(
                initialType = type, 
                initialText = prompt ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.YoungSelfLetters.route) {
            YoungSelfLetterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Questions.route) {
            QuestionsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Worlds.route) {
            WorldsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWorld = { category ->
                    // Pour le moment on reste sur l'écran
                }
            )
        }

        composable(Screen.Portraits.route) {
            PortraitScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Pact.route) {
            PactScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Essence.route) {
            EssencePortraitScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProtocol = { navController.navigate(Screen.ProtocolSettings.route) },
                onNavigateToAccessibility = { navController.navigate(Screen.AccessibilitySettings.route) },
                onNavigateToReconciliation = { navController.navigate(Screen.Reconciliation.route) }
            )
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
