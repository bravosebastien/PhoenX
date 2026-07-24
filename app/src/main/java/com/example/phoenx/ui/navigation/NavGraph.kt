package com.example.phoenx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.phoenx.ui.MainViewModel

@Composable
fun PhoenXNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        authGraph(navController, mainViewModel)
        creatorGraph(navController, mainViewModel)
        recipientGraph(navController, mainViewModel)
    }
}
