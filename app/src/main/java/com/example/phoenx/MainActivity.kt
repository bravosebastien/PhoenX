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
import com.example.phoenx.ui.screens.guide.WelcomeGuideScreen
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.components.RecoveryReminderDialog
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

    @androidx.media3.common.util.UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // CAPTUREUR DE CRASH POUR DEBUG
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("PHOENX_DEBUG", "FATAL CRASH sur le thread ${thread.name}")
            android.util.Log.e("PHOENX_DEBUG", "CAUSE: ${throwable.message}")
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            throwable.printStackTrace(pw)
            android.util.Log.e("PHOENX_DEBUG", sw.toString())
        }

        android.util.Log.d("PHOENX_DEBUG", "MainActivity onCreate")

        setContent {
            PhoenXTheme {
                val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
                val shouldShowGuide by mainViewModel.shouldShowWelcomeGuide.collectAsState()
                
                var isUnlocked by remember { mutableStateOf(value = false) }
                var showGuide by remember { mutableStateOf(value = false) }

                LaunchedEffect(isBiometricEnabled) {
                    android.util.Log.d("PHOENX_DEBUG", "Vérification Biométrie: enabled=$isBiometricEnabled")
                    if (isBiometricEnabled && !isUnlocked) {
                        biometricManager.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { 
                                android.util.Log.d("PHOENX_DEBUG", "Biométrie SUCCESS")
                                isUnlocked = true 
                            },
                            onError = { err -> 
                                android.util.Log.e("PHOENX_DEBUG", "Biométrie ERROR: $err")
                                isUnlocked = true // Fallback pour ne pas bloquer en debug
                            },
                        )
                    } else {
                        isUnlocked = true
                    }
                }

                // Une fois déverrouillé, on vérifie si on doit montrer le guide
                LaunchedEffect(isUnlocked, shouldShowGuide) {
                    if (isUnlocked && shouldShowGuide) {
                        android.util.Log.d("PHOENX_DEBUG", "Affichage Guide de bienvenue")
                        showGuide = true
                    }
                }

                if (showGuide) {
                    WelcomeGuideScreen { neverShowAgain ->
                        android.util.Log.d("PHOENX_DEBUG", "Guide terminé: neverShowAgain=$neverShowAgain")
                        mainViewModel.dismissWelcomeGuide(neverShowAgain)
                        showGuide = false
                    }
                } else if (isUnlocked) {
                    android.util.Log.d("PHOENX_DEBUG", "Chargement MainContent")
                    MainContent(
                        onVerifyBiometrics = { onSuccess ->
                            biometricManager.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = onSuccess,
                                onError = { /* handle error */ }
                            )
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = com.example.phoenx.ui.theme.BackgroundPrimary
                    ) {}
                }
            }
        }
    }

    @androidx.media3.common.util.UnstableApi
    @Composable
    fun MainContent(onVerifyBiometrics: (onSuccess: () -> Unit) -> Unit) {
        LaunchedEffect(Unit) {
            mainViewModel.confirmPresence()
        }

        val isVoiceActive by mainViewModel.isVoiceModeActive.collectAsState()
        val showRecoveryReminder by mainViewModel.showRecoveryReminder.collectAsState()
        val navController = rememberNavController()

        if (showRecoveryReminder) {
            RecoveryReminderDialog(
                onDismiss = { mainViewModel.dismissRecoveryReminder(false) },
                onConfirm = { 
                    mainViewModel.dismissRecoveryReminder(true)
                    navController.navigate(Screen.Settings.route + "?showRecovery=true")
                }
            )
        }

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
                mainViewModel = mainViewModel,
                onVerifyBiometrics = onVerifyBiometrics
            )
        }
    }
}
