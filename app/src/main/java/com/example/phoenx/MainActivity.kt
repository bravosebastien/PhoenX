package com.example.phoenx

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.biometric.PhoenXBiometricManager
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.navigation.PhoenXNavGraph
import com.example.phoenx.ui.screens.guide.WelcomeGuideScreen
import com.example.phoenx.ui.theme.PhoenXTheme
import com.example.phoenx.ui.theme.ThemeViewModel
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
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val accentColor by themeViewModel.accentColor.collectAsState()
            val backgroundColor by themeViewModel.backgroundColor.collectAsState()
            val backgroundStyle by themeViewModel.backgroundStyle.collectAsState()
            
            PhoenXTheme(
                accentColor = accentColor,
                backgroundColor = backgroundColor,
                backgroundStyle = backgroundStyle
            ) {
                val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
                val shouldShowGuide by mainViewModel.shouldShowWelcomeGuide.collectAsState()
                
                var isUnlocked by remember { mutableStateOf(value = false) }
                var showGuide by remember { mutableStateOf(value = false) }

                // LOGIQUE DE DÉVERROUILLAGE BIOMÉTRIQUE
                LaunchedEffect(isBiometricEnabled) {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (user != null && isBiometricEnabled && !isUnlocked) {
                        if (biometricManager.isBiometricAvailable()) {
                            biometricManager.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = { isUnlocked = true },
                                onError = { /* handle error */ },
                            )
                        } else {
                            isUnlocked = true
                        }
                    } else {
                        isUnlocked = true
                    }
                }

                // LOGIQUE DU GUIDE DE BIENVENUE
                LaunchedEffect(isUnlocked, shouldShowGuide) {
                    if (isUnlocked && shouldShowGuide == true) {
                        showGuide = true
                    }
                }

                if (isUnlocked && shouldShowGuide == null) {
                    // Attente du chargement des préférences
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(com.example.phoenx.ui.theme.LocalBackgroundBrush.current)
                    ) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                color = com.example.phoenx.ui.theme.AccentPrimary,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                } else if (showGuide) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(com.example.phoenx.ui.theme.LocalBackgroundBrush.current)
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
        // val showRecoveryReminder by mainViewModel.showRecoveryReminder.collectAsState() // Mis en veille
        val navController = rememberNavController()

        /*
        if (showRecoveryReminder) {
            RecoveryReminderDialog(
                onDismiss = { mainViewModel.dismissRecoveryReminder(false) },
                onConfirm = { 
                    mainViewModel.dismissRecoveryReminder(true)
                    navController.navigate(Screen.Settings.route + "?showRecovery=true")
                }
            )
        }
        */

        LaunchedEffect(isVoiceActive) {
            if (isVoiceActive) {
                // Vérifier la permission RECORD_AUDIO au runtime
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.RECORD_AUDIO
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.RECORD_AUDIO),
                        1001
                    )
                } else {
                    voiceManager.startListening { command ->
                        mainViewModel.handleVoiceCommand(command) { route ->
                            if (route == "back") navController.popBackStack()
                            else navController.navigate(route)
                        }
                    }
                }
            } else {
                voiceManager.stopListening()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.phoenx.ui.theme.LocalBackgroundBrush.current)
        ) {
            PhoenXNavGraph(
                navController = navController,
                mainViewModel = mainViewModel,
                onVerifyBiometrics = onVerifyBiometrics
            )
        }
    }
}
