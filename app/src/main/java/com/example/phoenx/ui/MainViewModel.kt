package com.example.phoenx.ui

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.domain.liveness.LivenessManager
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import com.example.phoenx.service.SilenceManager
import com.example.phoenx.service.SilenceStatus
import com.example.phoenx.ui.theme.AccentPrimary
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val protocolManager: ActivationProtocolManager,
    private val voiceManager: VoiceAccessibilityManager,
    private val livenessManager: LivenessManager,
    private val preferenceManager: PreferenceManager,
    private val silenceManager: SilenceManager
) : ViewModel() {

    private val _silenceStatus = MutableStateFlow<SilenceStatus?>(null)
    val silenceStatus: StateFlow<SilenceStatus?> = _silenceStatus.asStateFlow()

    private val _daysSinceLastCheckIn = MutableStateFlow(0)
    val daysSinceLastCheckIn: StateFlow<Int> = _daysSinceLastCheckIn.asStateFlow()
    
    val isSilenceOnboardingDone: StateFlow<Boolean?> = preferenceManager.isSilenceOnboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _silenceRhythmDays = MutableStateFlow(30)
    val silenceRhythmDays: StateFlow<Int> = _silenceRhythmDays.asStateFlow()

    val accentColor: StateFlow<Int> = preferenceManager.accentColor
        .map { it ?: AccentPrimary.toArgb() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentPrimary.toArgb())

    val backgroundColor: StateFlow<Int> = preferenceManager.backgroundColor
        .map { it ?: 0xFF00FFFF.toInt() } // Néon par défaut pour le fond (selon XML)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF00FFFF.toInt())

    val backgroundStyle: StateFlow<String> = preferenceManager.backgroundStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "RADIAL")

    val isVoiceModeActive: StateFlow<Boolean> = preferenceManager.isVoiceModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    init {
        checkInactivity()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            checkSilenceOnLaunch(userId)
        }
    }

    fun setAccentColor(color: Int) {
        viewModelScope.launch {
            preferenceManager.setAccentColor(color)
        }
    }

    fun setBackgroundColor(color: Int) {
        viewModelScope.launch {
            preferenceManager.setBackgroundColor(color)
        }
    }

    fun setBackgroundStyle(style: String) {
        viewModelScope.launch {
            preferenceManager.setBackgroundStyle(style)
        }
    }

    fun checkSilenceOnLaunch(userId: String) {
        viewModelScope.launch {
            val status = silenceManager.checkSilenceStatus(userId)
            _silenceStatus.value = status
            
            // Calculer les jours pour l'affichage
            try {
                val doc = db.collection("users").document(userId).get().await()
                
                // Charger le rythme
                val rhythm = doc.getLong("silenceConfig.rhythmDays")?.toInt() ?: 30
                _silenceRhythmDays.value = rhythm

                val lastCheckIn = doc.getTimestamp("silenceConfig.lastCheckInAt")
                if (lastCheckIn != null) {
                    val diff = System.currentTimeMillis() - lastCheckIn.toDate().time
                    _daysSinceLastCheckIn.value = (diff / (1000 * 60 * 60 * 24)).toInt()
                }
            } catch (e: Exception) {}
        }
    }

    fun recordCheckIn(status: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            silenceManager.recordCheckIn(userId, status)
            _silenceStatus.value = SilenceStatus.OK
        }
    }

    suspend fun setSilenceConfig(rhythmDays: Int) {
        val userId = auth.currentUser?.uid ?: return
        // Sauvegarder localement IMMEDIATEMENT pour stopper l'onboarding
        preferenceManager.setSilenceOnboardingDone(true)
        _silenceRhythmDays.value = rhythmDays
        
        try {
            db.collection("users").document(userId).update(
                mapOf(
                    "silenceConfig.rhythmDays" to rhythmDays,
                    "silenceConfig.lastCheckInAt" to com.google.firebase.Timestamp.now(),
                    "silenceConfig.missedCycles" to 0,
                    "silenceConfig.escalationLevel" to 0,
                    "silenceConfig.lastSilenceStatus" to "present"
                )
            ).await()
        } catch (e: Exception) {
            // Si Firestore échoue, on garde quand même le flag local pour ne pas harceler l'utilisateur
        }
    }

    val isBiometricEnabled: StateFlow<Boolean> = preferenceManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shouldShowWelcomeGuide: StateFlow<Boolean?> = preferenceManager.shouldShowWelcomeGuide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isVideoBannerDismissed: StateFlow<Boolean> = preferenceManager.isVideoBannerDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private fun checkInactivity() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            if (protocolManager.checkInactivity(userId)) {
                // Protocole en attente si nécessaire
            }
        }
    }

    fun confirmPresence() {
        livenessManager.confirmPassivePresence()
    }

    fun toggleVoiceMode() {
        viewModelScope.launch {
            val current = isVoiceModeActive.value
            preferenceManager.setVoiceModeActive(!current)
            if (!current) {
                voiceManager.speak("Mode vocal activé. Je vous écoute.")
            } else {
                voiceManager.speak("Mode vocal désactivé.")
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setBiometricEnabled(enabled)
        }
    }

    fun dismissWelcomeGuide(neverShowAgain: Boolean) {
        viewModelScope.launch {
            if (neverShowAgain) {
                preferenceManager.setShouldShowWelcomeGuide(false)
            }
        }
    }

    fun dismissVideoBanner() {
        viewModelScope.launch {
            preferenceManager.setVideoBannerDismissed(true)
        }
    }

    fun resetVideoBanner() {
        viewModelScope.launch {
            preferenceManager.setVideoBannerDismissed(false)
        }
    }

    fun isDepositaryOnboardingSeen(userId: String): Flow<Boolean> = 
        preferenceManager.isDepositaryOnboardingSeen(userId)

    fun markDepositaryOnboardingSeen(userId: String) {
        viewModelScope.launch {
            preferenceManager.setDepositaryOnboardingSeen(userId, true)
        }
    }

    fun handleVoiceCommand(command: String, navigate: (String) -> Unit) {
        val cmd = command.lowercase()
        when {
            cmd.contains("pensée") || cmd.contains("souvenir") || cmd.contains("écrire") -> navigate("capture/TEXT")
            cmd.contains("voix") || cmd.contains("enregistrer") -> navigate("capture/AUDIO")
            cmd.contains("photo") || cmd.contains("image") -> navigate("capture/PHOTO")
            cmd.contains("fil") || cmd.contains("timeline") || cmd.contains("historique") -> navigate("fil")
            cmd.contains("nuit") || cmd.contains("sommeil") -> navigate("capture/NIGHT")
            cmd.contains("accueil") || cmd.contains("maison") -> navigate("home")
            cmd.contains("ia") || cmd.contains("essence") || cmd.contains("portrait") -> navigate("essence")
            cmd.contains("meilleurs") || cmd.contains("favoris") -> navigate("favorites")
            cmd.contains("questions") || cmd.contains("histoire") -> navigate("questions")
            cmd.contains("aide") -> voiceManager.speak("Vous pouvez dire : 'Ouvre mon fil', 'Dépose une pensée', 'Enregistre ma voix', ou 'Retour à l'accueil'.")
            cmd.contains("retour") -> navigate("back")
        }
    }
    
    fun speak(text: String) {
        if (isVoiceModeActive.value) {
            voiceManager.speak(text)
        }
    }
}
