package com.example.phoenx.ui.screens.questions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
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
    val isSuccess: Boolean = false
)

@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
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

    fun saveAnswer(questionObj: Question, answer: String, mediaFile: File? = null) {
        val user = auth.currentUser ?: return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                val encrypted = encryptionManager.encryptText(answer)
                
                // GESTION DU MÉDIA LOCAL (Si présent)
                var finalLocalPath: String? = null
                if (mediaFile != null && mediaFile.exists()) {
                    val mediaDir = File(context.filesDir, "media")
                    if (!mediaDir.exists()) mediaDir.mkdirs()
                    val destFile = File(mediaDir, "PHX_Q_${UUID.randomUUID()}_${mediaFile.name}")
                    mediaFile.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    finalLocalPath = destFile.absolutePath
                }

                val entry = OfflineEntry(
                    creatorUid = user.uid,
                    encryptedPayload = encrypted,
                    entryType = "QUESTION_ANSWER",
                    questionId = questionObj.id, // v8.5.9 tracking
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = "Sagesse",
                    visibility = "RESTRICTED",
                    createdAt = System.currentTimeMillis(),
                    aiSummary = questionObj.text,
                    localMediaPath = finalLocalPath
                )
                offlineEntryDao.insertEntry(entry)

                // DECLENCHEMENT PIPELINE STANDARD (SyncWorker)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)

                // Mise à jour locale immédiate de l'état
                val newAnsweredIds = _uiState.value.answeredQuestionIds + questionObj.id
                _uiState.update { it.copy(isSaving = false, isSuccess = true, answeredQuestionIds = newAnsweredIds) }
            } catch (e: Exception) {
                android.util.Log.e("QuestionsVM", "Erreur sauvegarde: ${e.message}")
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
