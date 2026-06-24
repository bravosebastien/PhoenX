package com.example.phoenx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.navigation.PhoenXNavGraph
import com.example.phoenx.ui.theme.PhoenXTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var voiceManager: VoiceAccessibilityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoenXTheme {
                val isVoiceActive by mainViewModel.isVoiceModeActive.collectAsState()
                val navController = rememberNavController()

                // Si le mode vocal est actif, on écoute en permanence les commandes
                LaunchedEffect(isVoiceActive) {
                    if (isVoiceActive) {
                        voiceManager.startListening { command ->
                            mainViewModel.handleVoiceCommand(command) { route ->
                                navController.navigate(route)
                            }
                        }
                    } else {
                        voiceManager.stopListening()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.phoenx.ui.theme.BackgroundPrimary
                ) {
                    val startRoute = intent.getStringExtra("START_ROUTE")
                    val navController = rememberNavController()
                    
                    LaunchedEffect(startRoute) {
                        if (startRoute != null) {
                            navController.navigate(startRoute)
                        }
                    }

                    PhoenXNavGraph(
                        navController = navController,
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
    }
}
