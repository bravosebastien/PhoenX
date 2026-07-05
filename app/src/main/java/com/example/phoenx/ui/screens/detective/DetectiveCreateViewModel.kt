package com.example.phoenx.ui.screens.detective

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject

enum class ContentType { TEXT, AUDIO, PHOTO }

data class DetectiveCreateUiState(
    val enigmaText: String = "",
    val secretAnswer: String = "",
    val contentType: ContentType = ContentType.TEXT,
    val textContent: String = "",
    val audioUri: Uri? = null,
    val photoUri: Uri? = null,
    val isSaving: Boolean = false,
    val unlockAfterDays: Int = 30,
    val fallbackMessage: String = ""
)

@HiltViewModel
class DetectiveCreateViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectiveCreateUiState())
    val uiState: StateFlow<DetectiveCreateUiState> = _uiState.asStateFlow()

    fun updateEnigma(text: String) {
        _uiState.update { it.copy(enigmaText = text) }
    }

    fun updateAnswer(answer: String) {
        _uiState.update { it.copy(secretAnswer = answer) }
    }

    fun selectContentType(type: ContentType) {
        _uiState.update { it.copy(contentType = type) }
    }

    fun updateTextContent(text: String) {
        _uiState.update { it.copy(textContent = text) }
    }

    fun updatePhotoUri(uri: Uri?) {
        _uiState.update { it.copy(photoUri = uri) }
    }

    fun updateUnlockDays(days: Int) {
        _uiState.update { it.copy(unlockAfterDays = days) }
    }

    fun updateFallbackMessage(text: String) {
        _uiState.update { it.copy(fallbackMessage = text) }
    }

    fun hashAnswer(answer: String): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(answer.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun saveDetectiveEntry(onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val state = _uiState.value
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val hashedAnswer = hashAnswer(state.secretAnswer)
                
                // Préparation du contenu à chiffrer
                val contentToEncrypt = when(state.contentType) {
                    ContentType.TEXT -> state.textContent
                    ContentType.AUDIO -> "Audio record placeholder"
                    ContentType.PHOTO -> state.photoUri?.toString() ?: ""
                }
                
                val encryptedPayload = encryptionManager.encryptText(contentToEncrypt)
                
                // Chiffrement du message de révélation (fallbackAnswer)
                val encryptedFallback = if (state.fallbackMessage.isNotBlank()) {
                    android.util.Base64.encodeToString(
                        encryptionManager.encryptText(state.fallbackMessage),
                        android.util.Base64.DEFAULT
                    )
                } else null

                val userDoc = db.collection("users").document(userId).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: java.util.Date()
                val ageAtCreation = AgeUtils.calculateAge(birthDate)
                val ageJson = "{\"years\":${ageAtCreation.years},\"months\":${ageAtCreation.months},\"days\":${ageAtCreation.days}}"

                val entryData = hashMapOf(
                    "type" to state.contentType.name,
                    "encryptedContent" to android.util.Base64.encodeToString(encryptedPayload, android.util.Base64.DEFAULT),
                    "isDetective" to true,
                    "enigmaText" to state.enigmaText,
                    "hashedAnswer" to hashedAnswer,
                    "unlockAfterDays" to state.unlockAfterDays,
                    "fallbackAnswer" to encryptedFallback,
                    "ageAtCreation" to ageJson,
                    "emotionalCategory" to "Sagesse",
                    "aiSummary" to "",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("users").document(userId).collection("entries").add(entryData).await()
                
                offlineEntryDao.insertEntry(
                    OfflineEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        encryptedPayload = encryptedPayload,
                        entryType = state.contentType.name,
                        ageAtCreation = ageJson,
                        emotionalCategory = "Sagesse",
                        visibility = "Privé",
                        isYoungSelfLetter = false,
                        syncStatus = "synced",
                        enigmaQuestion = state.enigmaText,
                        enigmaAnswer = hashedAnswer,
                        unlockAfterDays = state.unlockAfterDays,
                        fallbackAnswer = encryptedFallback
                    )
                )

                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                android.util.Log.e("DetectiveVM", "Error saving enigma", e)
            }
        }
    }
}
