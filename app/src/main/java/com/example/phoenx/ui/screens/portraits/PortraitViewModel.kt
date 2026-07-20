package com.example.phoenx.ui.screens.portraits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PortraitEntity
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@HiltViewModel
class PortraitViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val db: com.google.firebase.firestore.FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortraitUiState>(PortraitUiState.Idle)
    val uiState: StateFlow<PortraitUiState> = _uiState

    private val _recipients = MutableStateFlow<List<RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<RecipientEntity>> = _recipients

    private val _currentRecipientId = MutableStateFlow<String?>(null)
    
    val existingPortrait: StateFlow<String?> = _currentRecipientId
        .filterNotNull()
        .flatMapLatest { id -> offlineEntryDao.getPortraitEntryForRecipient(id) }
        .map { entry -> 
            entry?.let { encryptionManager.decryptText(it.encryptedPayload) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collectLatest { list ->
                _recipients.value = list
            }
        }
    }

    fun setRecipient(recipientId: String?) {
        _currentRecipientId.value = recipientId
    }

    fun savePortrait(recipientId: String, questions: List<String>?, answers: List<String>) {
        _uiState.value = PortraitUiState.Loading
        viewModelScope.launch {
            try {
                if (answers.all { it.isBlank() } && (questions != null)) {
                    _uiState.value = PortraitUiState.Error("Le portrait est vide. Écris au moins une pensée.")
                    return@launch
                }

                val user = auth.currentUser ?: return@launch
                
                // Calcul de l'âge réel (v8.5.8)
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: java.util.Date()
                val age = com.example.phoenx.domain.util.AgeUtils.calculateAge(birthDate)
                val ageJson = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }"

                // 1. GESTION DE L'ENTRÉE PARENTE (Le Sceau du Portrait)
                val recipients = _recipients.value
                val recipientName = recipients.find { it.id == recipientId }?.name ?: "un proche"
                
                val existingParent = offlineEntryDao.getPortraitEntryForRecipient(recipientId).first()
                val parentId = existingParent?.id ?: UUID.randomUUID().toString()

                val parentEntry = OfflineEntry(
                    id = parentId,
                    creatorUid = user.uid,
                    encryptedPayload = "".toByteArray(),
                    entryType = "PORTRAIT",
                    ageAtCreation = ageJson, // Fix v8.5.8
                    emotionalCategory = "Amour",
                    visibility = "specific",
                    recipientIds = recipientId,
                    createdAt = existingParent?.createdAt ?: System.currentTimeMillis(),
                    aiSummary = "Portrait de $recipientName"
                )
                offlineEntryDao.insertEntry(parentEntry)

                // 2. GESTION DES RÉPONSES (Atomiques)
                if (questions != null) {
                    questions.forEachIndexed { index, question ->
                        val answer = answers.getOrNull(index) ?: ""
                        if (answer.isNotBlank()) {
                            val existingAnswers = offlineEntryDao.getComplements(parentId).first()
                            val existingAnswer = existingAnswers.find { it.aiSummary == question }
                            
                            val answerEntry = OfflineEntry(
                                id = existingAnswer?.id ?: UUID.randomUUID().toString(),
                                creatorUid = user.uid,
                                encryptedPayload = encryptionManager.encryptText(answer),
                                entryType = "TEXT",
                                parentEntryId = parentId,
                                ageAtCreation = ageJson, // Fix v8.5.8
                                emotionalCategory = "Amour",
                                visibility = "specific",
                                recipientIds = recipientId,
                                aiSummary = question
                            )
                            offlineEntryDao.insertEntry(answerEntry)
                        }
                    }
                }
                // ... (reste de la fonction savePortrait identique)
else {
                    // Cas "Pensée Libre" (PortraitProcheScreen)
                    val freeText = answers.joinToString("\n\n")
                    val existingAnswers = offlineEntryDao.getComplements(parentId).first()
                    val existingAnswer = existingAnswers.find { it.aiSummary == "Pensée libre" }

                    val answerEntry = OfflineEntry(
                        id = existingAnswer?.id ?: UUID.randomUUID().toString(),
                        creatorUid = parentEntry.creatorUid,
                        encryptedPayload = encryptionManager.encryptText(freeText),
                        entryType = "TEXT",
                        parentEntryId = parentId,
                        ageAtCreation = parentEntry.ageAtCreation,
                        emotionalCategory = "Amour",
                        visibility = "specific",
                        recipientIds = recipientId,
                        aiSummary = "Pensée libre"
                    )
                    offlineEntryDao.insertEntry(answerEntry)
                }

                // 3. SYNCHRONISATION
                triggerSync()
                _uiState.value = PortraitUiState.Success
            } catch (e: Exception) {
                _uiState.value = PortraitUiState.Error(e.message ?: "Erreur de sauvegarde")
            }
        }
    }

    private fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}

sealed class PortraitUiState {
    object Idle : PortraitUiState()
    object Loading : PortraitUiState()
    object Success : PortraitUiState()
    data class Error(val message: String) : PortraitUiState()
}
