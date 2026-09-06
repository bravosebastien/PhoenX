package com.example.phoenx.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.data.sync.toOfflineEntry
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.domain.util.EnigmaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class HundredQuestionsUiState(
    val questions: List<Question> = emptyList(),
    val answeredQuestionIds: Set<String> = emptySet(),
    val customQuestions: List<com.example.phoenx.data.local.OfflineEntry> = emptyList(),
    val selectedCategory: String = "Toutes",
    val isLoading: Boolean = false
)

@HiltViewModel
class HundredQuestionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    val mediaManager: MediaManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HundredQuestionsUiState())
    val uiState: StateFlow<HundredQuestionsUiState> = _uiState.asStateFlow()

    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAnsweredQuestions()
        loadCustomQuestions()
        observeLocalBadges()
        filterQuestions("Toutes")
    }

    private fun observeLocalBadges() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collect { entries ->
                val localAnsweredIds = entries.mapNotNull { it.questionId }.toSet()
                _uiState.update { it.copy(answeredQuestionIds = it.answeredQuestionIds + localAnsweredIds) }
            }
        }
    }

    private fun loadCustomQuestions() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collect { entries ->
                val custom = entries.filter { it.isGuessQuestion }
                _uiState.update { it.copy(customQuestions = custom) }
            }
        }
    }

    fun loadAnsweredQuestions() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Charger les questionId déjà répondus depuis Firestore
                val snapshot = db.collection("users").document(userId)
                    .collection("entries")
                    .whereNotEqualTo("questionId", null)
                    .get().await()
                
                val ids = snapshot.documents.mapNotNull { it.getString("questionId") }.toSet()
                _uiState.update { it.copy(answeredQuestionIds = ids, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun filterQuestions(category: String) {
        val filtered = if (category == "Toutes" || category == "Mes Questions") {
            QuestionsData.allQuestions
        } else {
            QuestionsData.allQuestions.filter { it.category == category }
        }
        _uiState.update { it.copy(questions = filtered, selectedCategory = category) }
    }

    fun saveCustomQuestion(
        id: String? = null,
        question: String,
        hint: String?,
        answer: String,
        story: String,
        recipientIds: List<String>,
        photoFile: java.io.File? = null
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                var photoPath: String? = null
                if (photoFile != null) {
                    photoPath = mediaManager.uploadCameo(userId, "custom_question_${System.currentTimeMillis()}", photoFile)
                }

                // CALCUL DE L'ÂGE
                val userDoc = db.collection("users").document(userId).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: java.util.Date()
                val age = AgeUtils.calculateAge(birthDate)
                val ageJson = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }"

                val encryptedPayload = encryptionManager.encryptText(story)
                val entry = com.example.phoenx.data.local.OfflineEntry(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    creatorUid = userId,
                    encryptedPayload = encryptedPayload,
                    entryType = "TEXT",
                    ageAtCreation = ageJson,
                    emotionalCategory = "Sagesse",
                    visibility = "RESTRICTED",
                    recipientIds = recipientIds.joinToString(","),
                    enigmaQuestion = question,
                    enigmaAnswer = EnigmaUtils.hashAnswer(answer),
                    enigmaHint = hint,
                    isGuessQuestion = true,
                    localMediaPath = photoFile?.absolutePath,
                    mediaUrl = photoPath,
                    userTitle = question,
                    syncStatus = "pending"
                )
                
                offlineEntryDao.insertEntry(entry)
                
                // Déclenchement sync
                SyncWorker.trigger(context)
            } catch (e: Exception) {
                android.util.Log.e("HundredQuestionsVM", "Erreur sauvegarde question personnalisée", e)
            }
        }
    }

    fun deleteCustomQuestion(entryId: String) {
        viewModelScope.launch {
            offlineEntryDao.deleteEntry(entryId)
        }
    }

    fun decryptStory(payload: ByteArray): String {
        return try {
            encryptionManager.decryptText(payload)
        } catch (e: Exception) {
            ""
        }
    }
}
