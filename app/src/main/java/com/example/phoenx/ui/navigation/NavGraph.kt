package com.example.phoenx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.phoenx.ui.MainViewModel
import androidx.media3.common.util.UnstableApi
import com.example.phoenx.ui.navigation.authGraph
import com.example.phoenx.ui.navigation.creatorGraph
import com.example.phoenx.ui.navigation.recipientGraph
import com.example.phoenx.ui.navigation.previewGraph

@UnstableApi
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
        previewGraph(navController, mainViewModel) // v9.4.27
    }
}
