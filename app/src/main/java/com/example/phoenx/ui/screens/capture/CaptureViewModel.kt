package com.example.phoenx.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.ai.OnDeviceAIManager
import com.example.phoenx.data.audio.PhoenXAudioRecorder
import com.example.phoenx.data.audio.SpeechToTextManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.haptic.HapticManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val aiManager: AIManager,
    private val onDeviceAIManager: OnDeviceAIManager,
    private val audioRecorder: PhoenXAudioRecorder,
    private val hapticManager: HapticManager,
    private val sttManager: SpeechToTextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _recipients = MutableStateFlow<List<RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<RecipientEntity>> = _recipients.asStateFlow()

    // Vocal
    val isSttListening = sttManager.isListening
    val sttPartialText = sttManager.partialText

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    init {
        loadRecipients()
    }

    private fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collect { list ->
                _recipients.value = list
            }
        }
    }

    fun startVocalCapture() {
        _transcript.value = ""
        sttManager.startListening { finalResult ->
            _transcript.value = finalResult
        }
    }

    fun stopVocalCapture() {
        sttManager.stopListening()
    }

    fun appendTranscript(text: String) {
        _transcript.value = text
    }

    private var currentAudioFile: File? = null

    fun startAudioRecording(cacheDir: File) {
        val file = File(cacheDir, "temp_capture_${System.currentTimeMillis()}.mp4")
        currentAudioFile = file
        audioRecorder.start(file)
        hapticManager.signalStartRecording()
        _uiState.value = CaptureUiState.RecordingAudio
    }

    fun stopAudioRecording() {
        audioRecorder.stop()
        _uiState.value = CaptureUiState.Idle
    }

    fun saveEntry(
        content: String?,
        mediaFile: File?,
        type: String,
        category: String,
        visibility: String,
        recipientIds: List<String> = emptyList(),
        isYoungSelfLetter: Boolean = false,
        targetAge: Int? = null,
        enigmaQuestion: String? = null,
        enigmaAnswer: String? = null,
        scheduledTimestamp: Long? = null,
        pactId: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ) {
        val user = auth.currentUser ?: return
        val rawText = content ?: if (type == Screen.Capture.TYPE_AUDIO) "Message vocal" else "Photo souvenir"
        _uiState.value = CaptureUiState.Loading

        viewModelScope.launch {
            try {
                // 1. ANALYSE IA LOCALE (Avant chiffrement)
                val analysis = onDeviceAIManager.analyzeLocally(rawText)

                // 2. CALCUL DE L'ÂGE
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                // 3. CHIFFREMENT E2EE
                val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
                
                // On chiffre le texte (légende ou contenu)
                val encrypted = encryptionManager.encryptText(rawText, tempKey)
                
                // 4. SAUVEGARDE HORS-LIGNE
                val entry = OfflineEntry(
                    encryptedPayload = encrypted,
                    entryType = type,
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = category,
                    visibility = visibility,
                    recipientIds = recipientIds.joinToString(","),
                    isYoungSelfLetter = isYoungSelfLetter,
                    targetAge = targetAge,
                    createdAt = System.currentTimeMillis(),
                    aiSummary = analysis.summary,
                    aiTags = analysis.tags.joinToString(","),
                    enigmaQuestion = enigmaQuestion,
                    enigmaAnswer = enigmaAnswer,
                    scheduledTimestamp = scheduledTimestamp,
                    pactId = pactId,
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName
                )
                offlineEntryDao.insertEntry(entry)
                
                // 5. SIGNAL PHYSIQUE
                hapticManager.signalSaveSuccess()
                _uiState.value = CaptureUiState.Success
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error(e.message ?: "Erreur lors du dépôt")
            }
        }
    }
}

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object RecordingAudio : CaptureUiState()
    object Loading : CaptureUiState()
    object Success : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}
