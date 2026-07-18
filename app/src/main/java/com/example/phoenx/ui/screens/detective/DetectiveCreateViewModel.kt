package com.example.phoenx.ui.screens.detective

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.EnigmaUtils
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import androidx.work.*
import com.example.phoenx.data.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val unlockAfterDays: Int = 30, // Délai actuel (Legacy)
    val enigmaHint: String = "",
    val autoUnlockDays: String = "", // Pour le nouveau champ optionnel
    val fallbackMessage: String = ""
)

@HiltViewModel
class DetectiveCreateViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    @ApplicationContext private val context: Context
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

    fun updateEnigmaHint(text: String) {
        _uiState.update { it.copy(enigmaHint = text) }
    }

    fun updateAutoUnlockDays(days: String) {
        _uiState.update { it.copy(autoUnlockDays = days) }
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
                // Normalisation et hachage unifiés (v8.3)
                val hashedAnswer = EnigmaUtils.hashAnswer(state.secretAnswer)
                val hashedFallback = EnigmaUtils.hashAnswer(state.fallbackMessage)
                
                // Préparation du contenu à chiffrer
                val contentToEncrypt = when(state.contentType) {
                    ContentType.TEXT -> state.textContent
                    ContentType.AUDIO -> "Audio record placeholder"
                    ContentType.PHOTO -> state.photoUri?.toString() ?: ""
                }
                
                val encryptedPayload = encryptionManager.encryptText(contentToEncrypt)
                
                val userDoc = db.collection("users").document(userId).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: java.util.Date()
                val ageAtCreation = AgeUtils.calculateAge(birthDate)
                val ageJson = "{\"years\":${ageAtCreation.years},\"months\":${ageAtCreation.months},\"days\":${ageAtCreation.days}}"

                offlineEntryDao.insertEntry(
                    OfflineEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        creatorUid = userId, // Ajout explicite requis pour le filtrage Room
                        encryptedPayload = encryptedPayload,
                        entryType = state.contentType.name,
                        ageAtCreation = ageJson,
                        emotionalCategory = "Sagesse",
                        visibility = "private",
                        isYoungSelfLetter = false,
                        syncStatus = "pending",
                        enigmaQuestion = state.enigmaText,
                        enigmaAnswer = hashedAnswer,
                        unlockAfterDays = state.unlockAfterDays,
                        fallbackAnswer = hashedFallback,
                        enigmaHint = state.enigmaHint.ifBlank { null },
                        enigmaAutoUnlockDays = state.autoUnlockDays.toIntOrNull()
                    )
                )

                // DECLENCHEMENT PIPELINE STANDARD (SyncWorker)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)

                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                android.util.Log.e("DetectiveVM", "Error saving enigma", e)
            }
        }
    }
}
