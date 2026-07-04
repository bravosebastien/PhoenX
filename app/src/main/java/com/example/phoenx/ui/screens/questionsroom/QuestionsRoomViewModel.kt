package com.example.phoenx.ui.screens.questionsroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.ui.screens.questions.Question
import com.example.phoenx.ui.screens.questions.QuestionsData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.example.phoenx.domain.model.PendingQuestion

data class QuestionsRoomUiState(
    val questions: List<Question> = emptyList(),
    val myPendingQuestions: List<PendingQuestion> = emptyList(),
    val answeredQuestionIds: Set<String> = emptySet(),
    val selectedCategory: String = "Toutes",
    val isLoading: Boolean = false,
    val creatorName: String = ""
)

@HiltViewModel
class QuestionsRoomViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionsRoomUiState())
    val uiState: StateFlow<QuestionsRoomUiState> = _uiState.asStateFlow()

    fun loadData(creatorId: String?) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (creatorId.isNullOrEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val targetCreatorId = creatorId
            
            try {
                // 1. Charger le nom du créateur
                val creatorDoc = db.collection("users").document(targetCreatorId).get().await()
                val name = creatorDoc.getString("displayName") ?: "Ton proche"
                _uiState.update { it.copy(creatorName = name) }

                // 2. Charger MES questions posées au créateur
                val questionsSnap = db.collection("users").document(targetCreatorId)
                    .collection("pendingQuestions")
                    .whereEqualTo("recipientId", currentUserId)
                    .get().await()
                
                val myQuestions = questionsSnap.documents.map { doc ->
                    doc.toObject(PendingQuestion::class.java)!!.copy(id = doc.id)
                }
                _uiState.update { it.copy(myPendingQuestions = myQuestions) }

                // 3. Charger les questions générales répondues (Fil de vie)
                val answeredSnap = db.collection("users").document(targetCreatorId)
                    .collection("entries")
                    .whereNotEqualTo("questionId", null)
                    .get().await()
                
                val ids = answeredSnap.documents.mapNotNull { it.getString("questionId") }.toSet()
                _uiState.update { it.copy(answeredQuestionIds = ids, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        filterQuestions("Toutes")
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
