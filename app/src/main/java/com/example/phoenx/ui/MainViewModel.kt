package com.example.phoenx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.accessibility.VoiceAccessibilityManager
import com.example.phoenx.domain.liveness.LivenessManager
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val protocolManager: ActivationProtocolManager,
    private val voiceManager: VoiceAccessibilityManager,
    private val livenessManager: LivenessManager
) : ViewModel() {

    private val _isVoiceModeActive = MutableStateFlow(false)
    val isVoiceModeActive: StateFlow<Boolean> = _isVoiceModeActive

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
        _isVoiceModeActive.value = !_isVoiceModeActive.value
        if (_isVoiceModeActive.value) {
            voiceManager.speak("Mode vocal activé. Je vous écoute.")
        }
    }

    fun handleVoiceCommand(command: String, navigate: (String) -> Unit) {
        when {
            command.contains("pensée") || command.contains("souvenir") -> navigate("capture/TEXT")
            command.contains("fil") || command.contains("timeline") -> navigate("fil")
            command.contains("nuit") -> navigate("capture/NIGHT")
            command.contains("accueil") -> navigate("home")
            command.contains("aide") -> voiceManager.speak("Dites 'Dépose une pensée', 'Ouvre mon fil' ou 'Mode nuit'.")
        }
    }
}
