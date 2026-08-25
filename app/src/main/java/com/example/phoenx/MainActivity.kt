package com.example.phoenx

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.biometric.PhoenXBiometricManager
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.screens.assistant.AssistantViewModel
import com.example.phoenx.ui.components.FloatingAssistantBubble
import com.example.phoenx.ui.screens.assistant.AssistantChatPanel
import com.example.phoenx.ui.navigation.PhoenXNavGraph
import com.example.phoenx.ui.screens.guide.WelcomeGuideScreen
import com.example.phoenx.ui.theme.PhoenXTheme
import com.example.phoenx.ui.theme.ThemeViewModel
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.components.rippleTrailDetection
import com.example.phoenx.ui.components.RippleTrailOverlay
import com.example.phoenx.ui.components.RippleTrailState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var navController: NavHostController? = null
    
    @Inject
    lateinit var voiceManager: VoiceAccessibilityManager

    @Inject
    lateinit var biometricManager: PhoenXBiometricManager

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // CAPTUREUR DE CRASH POUR DEBUG
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("PHOENX_DEBUG", "FATAL CRASH sur le thread ${thread.name}")
            android.util.Log.e("PHOENX_DEBUG", "CAUSE: ${throwable.message}")
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            throwable.printStackTrace(pw)
            android.util.Log.e("PHOENX_DEBUG", sw.toString())
            
            // On laisse le système gérer le crash après le log pour éviter le figement (v8.9.9)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        android.util.Log.d("PHOENX_DEBUG", "MainActivity onCreate")

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val accentColor by themeViewModel.accentColor.collectAsState()
            val backgroundId by themeViewModel.globalBackgroundId.collectAsState()
            val fontId by themeViewModel.globalFontId.collectAsState()
            val backgroundStyle by themeViewModel.backgroundStyle.collectAsState()
            
            PhoenXTheme(
                accentColor = accentColor,
                backgroundId = backgroundId,
                fontId = fontId,
                backgroundStyle = backgroundStyle
            ) {
                val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
                val shouldShowGuide by mainViewModel.shouldShowWelcomeGuide.collectAsState()
                
                var isUnlocked by remember { mutableStateOf(value = false) }
                var showGuide by remember { mutableStateOf(value = false) }

                // LOGIQUE DE DÉVERROUILLAGE BIOMÉTRIQUE
                LaunchedEffect(isBiometricEnabled) {
                    val user = FirebaseAuth.getInstance().currentUser
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
                            .background(LocalBackgroundBrush.current)
                    ) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                color = AccentPrimary,
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
                    MainContent(accentColor, showGuide)
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

    @Composable
    fun MainContent(accentColor: androidx.compose.ui.graphics.Color, isWelcomeGuideVisible: Boolean) {
        LaunchedEffect(Unit) {
            mainViewModel.confirmPresence()
        }

        val isVoiceActive by mainViewModel.isVoiceModeActive.collectAsState()
        val navController = rememberNavController()
        this.navController = navController

        val rippleState = remember { RippleTrailState() }
        
        // v9.4.29 : Centralisation de l'Assistant IA
        val assistantViewModel: AssistantViewModel = hiltViewModel()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        
        val assistantX by assistantViewModel.bubbleX.collectAsState()
        val assistantY by assistantViewModel.bubbleY.collectAsState()
        val isAssistantChatOpen by assistantViewModel.isChatOpen.collectAsState()

        val shouldShowAssistant = !isWelcomeGuideVisible && 
                                  currentRoute != null && 
                                  !currentRoute.startsWith("splash") &&
                                  !currentRoute.startsWith("onboarding") &&
                                  !currentRoute.startsWith("auth") &&
                                  !currentRoute.contains("CAMERA_PHOTO") &&
                                  !currentRoute.contains("CAMERA_VIDEO")

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
                .background(LocalBackgroundBrush.current)
                .rippleTrailDetection(rippleState, accentColor)
        ) {
            @androidx.media3.common.util.UnstableApi
            PhoenXNavGraph(
                navController = navController,
                mainViewModel = mainViewModel
            )

            // Overlay GLOBAL de traînée tactile (v9.2.7)
            RippleTrailOverlay(state = rippleState)
            
            // v9.4.29 : Assistant IA Global
            if (shouldShowAssistant) {
                FloatingAssistantBubble(
                    initialX = assistantX,
                    initialY = assistantY,
                    onPositionChanged = { x, y -> assistantViewModel.savePosition(x, y) },
                    onClick = { assistantViewModel.toggleChat() }
                )

                if (isAssistantChatOpen) {
                    AssistantChatPanel(
                        viewModel = assistantViewModel,
                        onDismiss = { assistantViewModel.toggleChat() }
                    )
                }
            }
        }
    }
}
