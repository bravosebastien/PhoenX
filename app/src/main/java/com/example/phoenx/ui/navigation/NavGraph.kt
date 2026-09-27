package com.example.phoenx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
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
    val context = LocalContext.current
    val analyticsTracker = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.example.phoenx.data.analytics.AnalyticsTracker.AnalyticsEntryPoint::class.java
        ).analyticsTracker()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            analyticsTracker.logScreenView(route)
        }
    }

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
