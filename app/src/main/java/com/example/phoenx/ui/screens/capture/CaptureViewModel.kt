package com.example.phoenx.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.audio.PhoenXAudioRecorder
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.haptic.HapticManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val audioRecorder: PhoenXAudioRecorder,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState

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
        audioFile: File?,
        type: String,
        category: String,
        visibility: String,
        isYoungSelfLetter: Boolean = false,
        targetAge: Int? = null
    ) {
        val user = auth.currentUser ?: return
        _uiState.value = CaptureUiState.Loading

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
                
                val payloadText = content ?: "Audio message"
                val encrypted = encryptionManager.encryptText(payloadText, tempKey)
                
                val entry = OfflineEntry(
                    encryptedPayload = encrypted,
                    entryType = type,
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = category,
                    visibility = visibility,
                    isYoungSelfLetter = isYoungSelfLetter,
                    targetAge = targetAge,
                    createdAt = System.currentTimeMillis()
                )
                offlineEntryDao.insertEntry(entry)
                
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
