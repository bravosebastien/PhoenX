package com.example.phoenx.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.domain.model.PendingQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PendingQuestionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _questions = MutableStateFlow<List<PendingQuestion>>(emptyList())
    val questions: StateFlow<List<PendingQuestion>> = _questions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadQuestions()
    }

    fun loadQuestions() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("pendingQuestions")
                    .orderBy("askedAt", Query.Direction.DESCENDING)
                    .get().await()
                
                val list = snapshot.documents.map { doc ->
                    doc.toObject(PendingQuestion::class.java)!!.copy(id = doc.id)
                }
                _questions.value = list
            } catch (e: Exception) {
                android.util.Log.e("PendingQuestionsVM", "Error loading questions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun declineQuestion(questionId: String, note: String?) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Chiffrement de la note avec Tink (AES-256-GCM)
                val encryptedNote = if (!note.isNullOrEmpty()) {
                    android.util.Base64.encodeToString(
                        encryptionManager.encryptText(note),
                        android.util.Base64.DEFAULT
                    )
                } else null

                db.collection("users").document(userId)
                    .collection("pendingQuestions").document(questionId)
                    .update(mapOf(
                        "status" to "declined",
                        "declineNote" to encryptedNote,
                        "answeredAt" to com.google.firebase.Timestamp.now()
                    )).await()
                loadQuestions()
            } catch (e: Exception) {
                android.util.Log.e("PendingQuestionsVM", "Error declining question", e)
            }
        }
    }
}
