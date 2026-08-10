package com.example.phoenx.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.book.BookReaderFlowScreen
import com.example.phoenx.ui.screens.preview.*
import com.example.phoenx.ui.util.NavigationAnimations
import com.example.phoenx.ui.theme.TransmissionTheme

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
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType }),
        enterTransition = { NavigationAnimations.getEnterTransition(this) },
        exitTransition = { NavigationAnimations.getExitTransition(this) },
        popEnterTransition = { NavigationAnimations.getPopEnterTransition(this) },
        popExitTransition = { NavigationAnimations.getPopExitTransition(this) }
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        
        // Randomize transitions when entering the preview space
        LaunchedEffect(Unit) { NavigationAnimations.randomize() }

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
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType }),
        enterTransition = { NavigationAnimations.getEnterTransition(this) },
        exitTransition = { NavigationAnimations.getExitTransition(this) },
        popEnterTransition = { NavigationAnimations.getPopEnterTransition(this) },
        popExitTransition = { NavigationAnimations.getPopExitTransition(this) }
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        val viewModel: PreviewViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        TransmissionTheme(
            backgroundId = state.ambiance.backgroundId,
            fontId = state.ambiance.fontId
        ) {
            PreviewFilScreen(
                recipientUid = recipientUid,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.Preview.MemoryDetail.createRoute(id, recipientUid)) }
            )
        }
    }

    composable(
        route = Screen.Preview.MemoryDetail.route,
        arguments = listOf(
            navArgument("entryId") { type = NavType.StringType },
            navArgument("recipientUid") { type = NavType.StringType }
        ),
        enterTransition = { NavigationAnimations.getEnterTransition(this) },
        exitTransition = { NavigationAnimations.getExitTransition(this) },
        popEnterTransition = { NavigationAnimations.getPopEnterTransition(this) },
        popExitTransition = { NavigationAnimations.getPopExitTransition(this) }
    ) { backStackEntry ->
        val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        val viewModel: PreviewViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        TransmissionTheme(
            backgroundId = state.ambiance.backgroundId,
            fontId = state.ambiance.fontId
        ) {
            PreviewMemoryDetailScreen(
                entryId = entryId,
                recipientUid = recipientUid,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.Preview.MemoryDetail.createRoute(id, recipientUid)) }
            )
        }
    }

    composable(
        route = Screen.Preview.Media.route,
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("recipientUid") { type = NavType.StringType }
        ),
        enterTransition = { NavigationAnimations.getEnterTransition(this) },
        exitTransition = { NavigationAnimations.getExitTransition(this) },
        popEnterTransition = { NavigationAnimations.getPopEnterTransition(this) },
        popExitTransition = { NavigationAnimations.getPopExitTransition(this) }
    ) { backStackEntry ->
        val type = backStackEntry.arguments?.getString("type") ?: "PHOTO"
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        val viewModel: PreviewViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        TransmissionTheme(
            backgroundId = state.ambiance.backgroundId,
            fontId = state.ambiance.fontId
        ) {
            PreviewMediaScreen(
                type = type,
                recipientUid = recipientUid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    composable(
        route = Screen.Preview.Book.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType }),
        enterTransition = { NavigationAnimations.getEnterTransition(this) },
        exitTransition = { NavigationAnimations.getExitTransition(this) },
        popEnterTransition = { NavigationAnimations.getPopEnterTransition(this) },
        popExitTransition = { NavigationAnimations.getPopExitTransition(this) }
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        val viewModel: PreviewViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        PreviewBookScreen(
            recipientUid = recipientUid,
            onNavigateBack = { navController.popBackStack() },
            onConsultBook = { 
                // Navigation vers le lecteur unifié avec paramètres d'aperçu
                navController.navigate(
                    "book_viewer_preview?recipientUid=$recipientUid" +
                    "&bgId=${state.ambiance.backgroundId}" +
                    "&fontId=${state.ambiance.fontId}"
                )
            }
        )
    }

    composable(
        route = Screen.Preview.Vault.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType }),
        enterTransition = { NavigationAnimations.getEnterTransition(this) },
        exitTransition = { NavigationAnimations.getExitTransition(this) },
        popEnterTransition = { NavigationAnimations.getPopEnterTransition(this) },
        popExitTransition = { NavigationAnimations.getPopExitTransition(this) }
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        val viewModel: PreviewViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        TransmissionTheme(
            backgroundId = state.ambiance.backgroundId,
            fontId = state.ambiance.fontId
        ) {
            PreviewVaultScreen(
                recipientUid = recipientUid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    composable(
        route = Screen.Preview.Genealogy.route,
        arguments = listOf(navArgument("recipientUid") { type = NavType.StringType }),
        enterTransition = { NavigationAnimations.getEnterTransition(this) },
        exitTransition = { NavigationAnimations.getExitTransition(this) },
        popEnterTransition = { NavigationAnimations.getPopEnterTransition(this) },
        popExitTransition = { NavigationAnimations.getPopExitTransition(this) }
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid") ?: ""
        val viewModel: PreviewViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        TransmissionTheme(
            backgroundId = state.ambiance.backgroundId,
            fontId = state.ambiance.fontId
        ) {
            PreviewGenealogyScreen(
                recipientUid = recipientUid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // --- LECTEUR UNIFIÉ EN MODE APERÇU (v9.4.27) ---
    composable(
        route = "book_viewer_preview?recipientUid={recipientUid}&bgId={bgId}&fontId={fontId}",
        arguments = listOf(
            navArgument("recipientUid") { type = NavType.StringType },
            navArgument("bgId") { type = NavType.StringType },
            navArgument("fontId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val recipientUid = backStackEntry.arguments?.getString("recipientUid")
        val bgId = backStackEntry.arguments?.getString("bgId")
        val fontId = backStackEntry.arguments?.getString("fontId")
        
        BookReaderFlowScreen(
            navController = navController,
            simulatedRecipientUid = recipientUid,
            forcedAmbiance = com.example.phoenx.ui.screens.recipient.AmbianceState(
                backgroundId = bgId ?: "classic_ivory",
                fontId = fontId ?: "playfair_display"
            )
        )
    }
}
