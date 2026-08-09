package com.example.phoenx.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.preview.PreviewDashboardScreen
import com.example.phoenx.ui.screens.preview.PreviewFilScreen
import com.example.phoenx.ui.screens.preview.PreviewMediaScreen
import com.example.phoenx.ui.screens.preview.PreviewBookScreen
import com.example.phoenx.ui.screens.preview.PreviewVaultScreen
import com.example.phoenx.ui.screens.preview.PreviewGenealogyScreen
import com.example.phoenx.ui.screens.preview.PreviewMemoryDetailScreen

/**
 * PreviewGraph (v9.4.27)
 * Graphe de navigation dédié au mode "Aperçu Vision Destinataire".
 * RÈGLE : Isolation totale des écrans de production.
 */
fun NavGraphBuilder.previewGraph(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    composable(
        route = Screen.Preview.Root.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType })
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        
        PreviewDashboardScreen(
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToFil = { navController.navigate(Screen.Preview.Fil.createRoute(recipientUid)) },
            onNavigateToMedia = { type -> navController.navigate(Screen.Preview.Media.createRoute(type, recipientUid)) },
            onNavigateToBook = { navController.navigate(Screen.Preview.Book.createRoute(recipientUid)) },
            onNavigateToVault = { navController.navigate(Screen.Preview.Vault.createRoute(recipientUid)) },
            onNavigateToGenealogy = { navController.navigate(Screen.Preview.Genealogy.createRoute(recipientUid)) }
        )
    }

    composable(
        route = Screen.Preview.Fil.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType })
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        PreviewFilScreen(
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDetail = { id -> navController.navigate(Screen.Preview.MemoryDetail.createRoute(id, recipientUid)) }
        )
    }

    composable(
        route = Screen.Preview.MemoryDetail.route,
        arguments = listOf(
            navArgument("entryId") { type = NavType.StringType },
            navArgument("recipientUid") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        PreviewMemoryDetailScreen(
            entryId = entryId,
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.Preview.Media.route,
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("recipientUid") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val type = backStackEntry.arguments?.getString("type") ?: "PHOTO"
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        PreviewMediaScreen(
            type = type,
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.Preview.Book.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType })
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        PreviewBookScreen(
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.Preview.Vault.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType })
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        PreviewVaultScreen(
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.Preview.Genealogy.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType })
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        PreviewGenealogyScreen(
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
