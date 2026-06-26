package com.example.phoenx

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.biometric.PhoenXBiometricManager
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.navigation.PhoenXNavGraph
import com.example.phoenx.ui.theme.PhoenXTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var voiceManager: VoiceAccessibilityManager

    @Inject
    lateinit var biometricManager: PhoenXBiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoenXTheme {
                val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
                var isUnlocked by remember { mutableStateOf(false) }

                LaunchedEffect(isBiometricEnabled) {
                    if (isBiometricEnabled && !isUnlocked) {
                        biometricManager.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { isUnlocked = true },
                            onError = { /* Log error, fallback to password */ }
                        )
                    } else {
                        isUnlocked = true
                    }
                }

                if (isUnlocked) {
                    MainContent()
                } else {
                    // Lock screen waiting for fingerprint
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = com.example.phoenx.ui.theme.BackgroundPrimary
                    ) {}
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        // Silent Proof of Life update
        LaunchedEffect(Unit) {
            mainViewModel.confirmPresence()
        }

        val isVoiceActive by mainViewModel.isVoiceModeActive.collectAsState()
        val navController = rememberNavController()

        LaunchedEffect(isVoiceActive) {
            if (isVoiceActive) {
                voiceManager.startListening { command ->
                    mainViewModel.handleVoiceCommand(command) { route ->
                        if (route == "back") navController.popBackStack()
                        else navController.navigate(route)
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
            PhoenXNavGraph(
                navController = navController,
                mainViewModel = mainViewModel
            )
        }
    }
}
