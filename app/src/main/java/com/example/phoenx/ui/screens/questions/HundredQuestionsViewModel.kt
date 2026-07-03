package com.example.phoenx.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class HundredQuestionsUiState(
    val questions: List<Question> = emptyList(),
    val answeredQuestionIds: Set<String> = emptySet(),
    val selectedCategory: String = "Toutes",
    val isLoading: Boolean = false
)

@HiltViewModel
class HundredQuestionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HundredQuestionsUiState())
    val uiState: StateFlow<HundredQuestionsUiState> = _uiState.asStateFlow()

    init {
        loadAnsweredQuestions()
        filterQuestions("Toutes")
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
        val filtered = if (category == "Toutes") {
            QuestionsData.allQuestions
        } else {
            QuestionsData.allQuestions.filter { it.category == category }
        }
        _uiState.update { it.copy(questions = filtered, selectedCategory = category) }
    }
}
