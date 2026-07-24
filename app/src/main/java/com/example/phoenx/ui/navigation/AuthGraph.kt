package com.example.phoenx.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.auth.AuthScreen
import com.example.phoenx.ui.screens.onboarding.OnboardingScreen
import com.example.phoenx.ui.screens.recovery.RecoveryScreen
import com.example.phoenx.ui.screens.splash.SplashScreen
import com.google.firebase.auth.FirebaseAuth

fun NavGraphBuilder.authGraph(
    navController: NavController,
    mainViewModel: MainViewModel
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
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    mainViewModel.checkSilenceOnLaunch(uid)
                }
                
                if (redirectTo != null) {
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
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    mainViewModel.checkSilenceOnLaunch(uid)
                }
                
                if (redirectTo != null) {
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

    composable(Screen.Auth.Recovery.route) {
        RecoveryScreen(
            onNavigateBack = { navController.popBackStack() },
            onSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
        )
    }
}
