package com.example.phoenx

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var navController: NavHostController? = null
    
    @Inject
    lateinit var voiceManager: VoiceAccessibilityManager

    @Inject
    lateinit var biometricManager: PhoenXBiometricManager

    companion object {
        private var isProcessObserverRegistered = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // v12.7.1 : Sécurité — redemander la biométrie à chaque retour au premier plan.
        // ProcessLifecycleOwner suit le cycle de vie de l'application entière. On ne repasse
        // isUnlocked à false que si le verrouillage est activé en réglages.
        if (!isProcessObserverRegistered) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    if (mainViewModel.isBiometricEnabled.value) {
                        mainViewModel.setUnlocked(false)
                    }
                }
            })
            isProcessObserverRegistered = true
        }
        
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
            val backgroundColor by themeViewModel.globalBackgroundColor.collectAsState()
            val fontId by themeViewModel.globalFontId.collectAsState()
            
            PhoenXTheme(
                accentColor = accentColor,
                backgroundColor = backgroundColor,
                fontId = fontId
            ) {
                val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
                val shouldShowWelcomeGuide by mainViewModel.shouldShowWelcomeGuide.collectAsState()

                val isUnlocked by mainViewModel.isUnlocked.collectAsState()
                var showGuide by remember { mutableStateOf(value = false) }
                var biometricFailed by remember { mutableStateOf(false) }
                var showRetryButton by remember { mutableStateOf(false) }

                // v12.7.7 : Filet de sécurité temporel pour le bouton "Réessayer"
                LaunchedEffect(isUnlocked) {
                    if (!isUnlocked) {
                        kotlinx.coroutines.delay(1500)
                        if (!isUnlocked) showRetryButton = true
                    } else {
                        showRetryButton = false
                    }
                }

                // LOGIQUE DE DÉVERROUILLAGE BIOMÉTRIQUE
                LaunchedEffect(isBiometricEnabled, isUnlocked) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null && isBiometricEnabled && !isUnlocked) {
                        if (biometricManager.isBiometricAvailable()) {
                            while (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                                kotlinx.coroutines.delay(100)
                            }
                            biometricManager.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = { 
                                    mainViewModel.setUnlocked(true)
                                    biometricFailed = false
                                    showRetryButton = false
                                },
                                onError = { biometricFailed = true },
                            )
                        } else {
                            mainViewModel.setUnlocked(true)
                        }
                    } else if (user != null && !isBiometricEnabled) {
                        mainViewModel.setUnlocked(true)
                    } else if (user == null) {
                        mainViewModel.setUnlocked(true)
                    }
                }

                // LOGIQUE DU GUIDE DE BIENVENUE
                LaunchedEffect(isUnlocked, shouldShowWelcomeGuide) {
                    if (isUnlocked && shouldShowWelcomeGuide == true) {
                        showGuide = true
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. BASE LAYER : MainContent (Toujours monté dès que les prefs sont là v12.7.8)
                    if (shouldShowWelcomeGuide != null) {
                        MainContent(accentColor, showGuide)
                    }

                    // 2. OVERLAY : CHARGEMENT INITIAL (shouldShowWelcomeGuide est null au démarrage)
                    if (shouldShowWelcomeGuide == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(LocalBackgroundBrush.current)
                                .zIndex(100f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentPrimary,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    // 3. OVERLAY : GUIDE DE BIENVENUE
                    if (showGuide) {
                        Box(modifier = Modifier.fillMaxSize().zIndex(50f)) {
                            WelcomeGuideScreen { neverShowAgain ->
                                android.util.Log.d("PHOENX_DEBUG", "Guide terminé: neverShowAgain=$neverShowAgain")
                                mainViewModel.dismissWelcomeGuide(neverShowAgain)
                                showGuide = false
                            }
                        }
                    }

                    // 4. OVERLAY : ÉCRAN DE VERROUILLAGE (Priorité maximale)
                    if (!isUnlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(LocalBackgroundBrush.current)
                                .zIndex(200f)
                                .pointerInput(Unit) { detectTapGestures { } },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(64.dp), 
                                    tint = LocalAppTheme.current.contentColor.copy(alpha = 0.2f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text("Déverrouillage nécessaire", color = LocalAppTheme.current.contentColor.copy(alpha = 0.5f))
                                
                                if (biometricFailed || showRetryButton) {
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            biometricManager.showBiometricPrompt(
                                                activity = this@MainActivity,
                                                onSuccess = { 
                                                    mainViewModel.setUnlocked(true)
                                                    biometricFailed = false
                                                    showRetryButton = false
                                                },
                                                onError = { biometricFailed = true }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LocalAppTheme.current.accentColor
                                        )
                                    ) {
                                        Text("Réessayer", color = LocalAppTheme.current.backgroundColor)
                                    }
                                }
                            }
                        }
                    }
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
                                  !currentRoute.contains("CAMERA_VIDEO") &&
                                  !currentRoute.startsWith("book_reader")

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
