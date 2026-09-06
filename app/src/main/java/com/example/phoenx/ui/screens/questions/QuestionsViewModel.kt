package com.example.phoenx.ui.screens.questions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.sync.toOfflineEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
import javax.inject.Inject

data class QuestionsUiState(
    val questions: List<Question> = emptyList(),
    val answeredQuestionIds: Set<String> = emptySet(),
    val selectedCategory: String = "Toutes",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val currentAnswerText: String? = null, // null = en chargement, "" = vide
    val currentMediaPath: String? = null,
    val currentMediaUrl: String? = null
)

@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    val mediaManager: MediaManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionsUiState())
    val uiState: StateFlow<QuestionsUiState> = _uiState.asStateFlow()

    init {
        loadAnsweredQuestions()
        filterQuestions("Toutes")
    }

    fun loadAnsweredQuestions() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Charger les questionId déjà répondus depuis Firestore (v8.5.9)
                val snapshot = db.collection("users").document(userId)
                    .collection("entries")
                    .whereNotEqualTo("questionId", null)
                    .get().await()
                
                val ids = snapshot.documents.mapNotNull { it.getString("questionId") }.toSet()
                _uiState.update { it.copy(answeredQuestionIds = ids, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("QuestionsVM", "Erreur chargement réponses: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun filterQuestions(category: String) {
        val filtered = if (category == "Toutes") {
            QuestionsData.allQuestions
        } else {
            QuestionsData.allQuestions.filter { it.category == category }
        }
        _uiState.update { it.copy(questions = filtered, selectedCategory = category) }
    }

    fun loadExistingAnswer(questionId: String) {
        viewModelScope.launch {
            android.util.Log.d("QuestionsVM", "Chargement réponse pour questionId: $questionId")
            val entry = offlineEntryDao.getEntryByQuestionIdSync(questionId)
            if (entry != null) {
                android.util.Log.d("QuestionsVM", "Entrée trouvée localement: ${entry.id}")
                val decodedText = try {
                    encryptionManager.decryptText(entry.encryptedPayload)
                } catch (e: Exception) { 
                    android.util.Log.e("QuestionsVM", "Échec déchiffrement", e)
                    "" 
                }
                
                _uiState.update { it.copy(
                    currentAnswerText = decodedText,
                    currentMediaPath = entry.localMediaPath,
                    currentMediaUrl = entry.mediaUrl
                ) }
            } else {
                android.util.Log.w("QuestionsVM", "Aucune réponse locale trouvée pour $questionId. Tentative Firestore...")
                
                // Fallback Firestore (v12.3.1 : Correction rechargement)
                val userId = auth.currentUser?.uid ?: return@launch
                try {
                    val snapshot = db.collection("users").document(userId)
                        .collection("entries")
                        .whereEqualTo("questionId", questionId)
                        .limit(1)
                        .get().await()
                    
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        val remoteEntry = doc.toOfflineEntry(encryptionManager)
                        if (remoteEntry != null) {
                            val decodedText = try {
                                encryptionManager.decryptText(remoteEntry.encryptedPayload)
                            } catch (e: Exception) { "" }
                            
                            _uiState.update { it.copy(
                                currentAnswerText = decodedText,
                                currentMediaPath = null,
                                currentMediaUrl = remoteEntry.mediaUrl
                            ) }
                            
                            // Réparation locale immédiate (Point 1 du bug critique)
                            offlineEntryDao.insertEntry(remoteEntry)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuestionsVM", "Erreur fallback Firestore", e)
                }

                _uiState.update { it.copy(
                    currentAnswerText = "",
                    currentMediaPath = null,
                    currentMediaUrl = null
                ) }
            }
        }
    }

    fun saveAnswer(questionObj: Question, answer: String, photoFile: File? = null) {
        val user = auth.currentUser ?: return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                // 1. Vérifier si une réponse existe déjà pour réutilisation de l'ID
                val existingEntry = offlineEntryDao.getEntryByQuestionIdSync(questionObj.id)
                val entryId = existingEntry?.id ?: UUID.randomUUID().toString()

                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                val encrypted = encryptionManager.encryptText(answer)
                
                // 2. GESTION DU MÉDIA (Upload chiffré v12.3.2)
                var photoPath = existingEntry?.mediaUrl
                var finalLocalPath = existingEntry?.localMediaPath
                
                if (photoFile != null && photoFile.exists()) {
                    // Upload chiffré vers Storage (Règle standard souvenirs)
                    android.util.Log.d("QuestionsVM", "Début upload chiffré pour $entryId")
                    photoPath = mediaManager.encryptAndUpload(user.uid, entryId, photoFile)
                    
                    // Copie locale persistante
                    val mediaDir = File(context.filesDir, "media")
                    if (!mediaDir.exists()) mediaDir.mkdirs()
                    val destFile = File(mediaDir, "PHX_QFIX_${entryId}.jpg")
                    photoFile.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    finalLocalPath = destFile.absolutePath
                }

                val entry = OfflineEntry(
                    id = entryId,
                    creatorUid = user.uid,
                    encryptedPayload = encrypted,
                    entryType = "QUESTION_ANSWER",
                    questionId = questionObj.id,
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = "Sagesse",
                    visibility = "RESTRICTED",
                    createdAt = existingEntry?.createdAt ?: System.currentTimeMillis(),
                    aiSummary = questionObj.text,
                    userTitle = questionObj.text, // v12.3.2 : Titre explicite pour l'IA
                    localMediaPath = finalLocalPath,
                    mediaUrl = photoPath,
                    syncStatus = "pending"
                )
                offlineEntryDao.insertEntry(entry)

                // 3. DECLENCHEMENT PIPELINE STANDARD (SyncWorker)
                SyncWorker.trigger(context)

                // Mise à jour locale immédiate de l'état
                val newAnsweredIds = _uiState.value.answeredQuestionIds + questionObj.id
                _uiState.update { it.copy(isSaving = false, isSuccess = true, answeredQuestionIds = newAnsweredIds) }
            } catch (e: Exception) {
                android.util.Log.e("QuestionsVM", "Erreur sauvegarde: ${e.message}")
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun removeExistingPhoto(questionId: String) {
        viewModelScope.launch {
            try {
                val entry = offlineEntryDao.getEntryByQuestionIdSync(questionId)
                if (entry != null) {
                    // Mise à jour locale et marquage pour sync
                    val updated = entry.copy(
                        mediaUrl = null,
                        localMediaPath = null,
                        syncStatus = "pending"
                    )
                    offlineEntryDao.insertEntry(updated)
                    
                    _uiState.update { it.copy(
                        currentMediaPath = null,
                        currentMediaUrl = null
                    ) }
                    
                    SyncWorker.trigger(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("QuestionsVM", "Erreur suppression photo", e)
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
