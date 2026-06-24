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
import com.example.phoenx.ui.screens.auth.AuthScreen
import com.example.phoenx.ui.screens.capture.CaptureScreen
import com.example.phoenx.ui.screens.depositary.DepositaryScreen
import com.example.phoenx.ui.screens.fil.FilScreen
import com.example.phoenx.ui.screens.home.HomeScreen
import com.example.phoenx.ui.screens.onboarding.OnboardingScreen
import com.example.phoenx.ui.screens.pact.PactScreen
import com.example.phoenx.ui.screens.portraits.PortraitScreen
import com.example.phoenx.ui.screens.youngselfletters.YoungSelfLetterScreen
import com.example.phoenx.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PhoenXNavGraph(navController: NavHostController) {
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
                onNavigateToCapture = { type -> navController.navigate(Screen.Capture.createRoute(type)) },
                onNavigateToFil = { navController.navigate(Screen.Fil.route) },
                onNavigateToLetters = { navController.navigate(Screen.YoungSelfLetters.route) }
            )
        }

        composable(Screen.Fil.route) {
            FilScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(
            route = Screen.Capture.route,
            arguments = listOf(navArgument("type") { defaultValue = Screen.Capture.TYPE_TEXT })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: Screen.Capture.TYPE_TEXT
            CaptureScreen(initialType = type, onNavigateBack = { navController.popBackStack() })
        }
        
        composable(Screen.YoungSelfLetters.route) {
            YoungSelfLetterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Portraits.route) {
            PortraitScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Pact.route) {
            PactScreen(onNavigateBack = { navController.popBackStack() })
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
