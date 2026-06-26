package com.example.phoenx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.domain.liveness.LivenessManager
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val protocolManager: ActivationProtocolManager,
    private val voiceManager: VoiceAccessibilityManager,
    private val livenessManager: LivenessManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val isVoiceModeActive: StateFlow<Boolean> = preferenceManager.isVoiceModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBiometricEnabled: StateFlow<Boolean> = preferenceManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shouldShowWelcomeGuide: StateFlow<Boolean> = preferenceManager.shouldShowWelcomeGuide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        checkInactivity()
    }

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
        if (neverShowAgain) {
            viewModelScope.launch {
                preferenceManager.setShouldShowWelcomeGuide(false)
            }
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
