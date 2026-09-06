package com.example.phoenx.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LeaderboardUiState(
    val results: List<GuessRecipientResult> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HundredQuestionsLeaderboardViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    fun loadResults(creatorId: String?) {
        val userId = creatorId ?: auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Charger les résultats de devinette
                val snapshot = db.collection("users").document(userId)
                    .collection("guessResults")
                    .orderBy("completedAt", Query.Direction.DESCENDING)
                    .get().await()
                
                val rawResults = snapshot.documents.map { doc ->
                    val data = doc.data!!
                    mapOf(
                        "recipientId" to (data["recipientId"] as String),
                        "recipientName" to (data["recipientName"] as String),
                        "entryId" to (data["entryId"] as String),
                        "isCorrect" to (data["isCorrect"] as Boolean),
                        "attemptCount" to (data["attemptCount"] as Number).toInt()
                    )
                }

                // 2. Charger les questions personnalisées locales pour le texte (ou distantes si mode héritier)
                val questionsMap = if (creatorId == null || creatorId == auth.currentUser?.uid) {
                    offlineEntryDao.getAllEntriesSync().filter { it.isGuessQuestion }.associateBy { it.id }
                } else {
                    val qSnap = db.collection("users").document(userId)
                        .collection("entries")
                        .whereEqualTo("isGuessQuestion", true)
                        .get().await()
                    qSnap.documents.associate { it.id to (it.getString("enigmaQuestion") ?: "Question inconnue") }
                }

                // 3. Grouper et agréger
                val grouped = rawResults.groupBy { it["recipientId"] as String }
                val aggregated = grouped.map { (id, items) ->
                    val name = items.first()["recipientName"] as String
                    val totalCorrect = items.filter { it["isCorrect"] as Boolean }.map { it["entryId"] }.distinct().size
                    
                    val details = items.map { item ->
                        val entryId = item["entryId"] as String
                        val questionText = if (questionsMap[entryId] is String) {
                            questionsMap[entryId] as String
                        } else {
                            (questionsMap[entryId] as? com.example.phoenx.data.local.OfflineEntry)?.enigmaQuestion ?: "Question inconnue"
                        }
                        
                        GuessDetail(
                            question = questionText,
                            isCorrect = item["isCorrect"] as Boolean,
                            attempts = item["attemptCount"] as Int
                        )
                    }
                    
                    GuessRecipientResult(
                        recipientId = id,
                        recipientName = name,
                        totalCorrect = totalCorrect,
                        details = details
                    )
                }.sortedByDescending { it.totalCorrect }

                _uiState.update { it.copy(results = aggregated, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("LeaderboardVM", "Erreur chargement classement", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
