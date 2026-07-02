package com.example.phoenx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.domain.liveness.LivenessManager
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import com.example.phoenx.domain.manager.SilenceManager
import com.example.phoenx.domain.manager.SilenceStatus
import com.example.phoenx.domain.models.SilenceConfig
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _silenceStatus = MutableStateFlow<SilenceStatus>(SilenceStatus.OK)
    val silenceStatus: StateFlow<SilenceStatus> = _silenceStatus.asStateFlow()

    private val _daysSinceLastCheckIn = MutableStateFlow(0)
    val daysSinceLastCheckIn: StateFlow<Int> = _daysSinceLastCheckIn.asStateFlow()

    private val _showRecoveryReminder = MutableStateFlow(false)
    val showRecoveryReminder: StateFlow<Boolean> = _showRecoveryReminder.asStateFlow()

    val isVoiceModeActive: StateFlow<Boolean> = preferenceManager.isVoiceModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // ... rest of state flows ...

    init {
        checkInactivity()
        checkSilence()
        checkRecoveryReminder()
    }

    private fun checkRecoveryReminder() {
        viewModelScope.launch {
            preferenceManager.lastRecoveryReminder.collect { lastReminder ->
                val sixMonthsMillis = 180L * 24 * 60 * 60 * 1000
                if (System.currentTimeMillis() - lastReminder > sixMonthsMillis) {
                    _showRecoveryReminder.value = true
                }
            }
        }
    }

    fun dismissRecoveryReminder(confirmed: Boolean) {
        _showRecoveryReminder.value = false
        if (confirmed) {
            viewModelScope.launch {
                preferenceManager.updateLastRecoveryReminder(System.currentTimeMillis())
            }
        }
    }

    private fun checkSilence() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                val rhythmDays = doc.getLong("silenceConfig.rhythmDays")?.toInt() ?: 30
                val lastCheckInAt = doc.getTimestamp("silenceConfig.lastCheckInAt") ?: com.google.firebase.Timestamp.now()
                val missedCycles = doc.getLong("silenceConfig.missedCycles")?.toInt() ?: 0
                val lastSilenceStatus = doc.getString("silenceConfig.lastSilenceStatus") ?: "present"

                val config = SilenceConfig(rhythmDays, lastCheckInAt, missedCycles, lastSilenceStatus)
                val status = SilenceManager.checkSilenceStatus(config)
                
                _silenceStatus.value = status
                
                // Calculer les jours pour l'écran de blocage
                val diff = System.currentTimeMillis() - lastCheckInAt.toDate().time
                _daysSinceLastCheckIn.value = (diff / (1000 * 60 * 60 * 24)).toInt()

            } catch (e: Exception) {
                // Erreur ou config inexistante
            }
        }
    }

    fun recordCheckIn(status: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            SilenceManager.recordCheckIn(userId, status, db)
            _silenceStatus.value = SilenceStatus.OK
        }
    }

    fun setSilenceConfig(rhythmDays: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val config = SilenceConfig(rhythmDays = rhythmDays)
            db.collection("users").document(userId).update("silenceConfig", config).await()
        }
    }

    val isBiometricEnabled: StateFlow<Boolean> = preferenceManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shouldShowWelcomeGuide: StateFlow<Boolean> = preferenceManager.shouldShowWelcomeGuide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val recoveryPhrase: Flow<String?> = preferenceManager.recoveryPhrase

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

    /**
     * PREUVE DE VIE PASSIVE : Confirme la présence du créateur silencieusement.
     */
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
