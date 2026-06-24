package com.example.phoenx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.phoenx.ui.screens.TimelineScreen
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.example.phoenx.ui.theme.TextPrimary

@Composable
fun PhoenXNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Timeline.route
    ) {
        composable(Screen.Timeline.route) {
            TimelineScreen()
        }
        composable(Screen.Auth.route) {
            PlaceholderScreen("Authentification")
        }
        composable(Screen.AddEntry.route) {
            PlaceholderScreen("Nouvelle Pensée")
        }
        composable(Screen.Cockpit.route) {
            PlaceholderScreen("Le Cockpit")
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name, color = TextPrimary)
    }
}
