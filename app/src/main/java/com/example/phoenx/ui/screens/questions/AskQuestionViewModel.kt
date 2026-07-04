package com.example.phoenx.ui.screens.questions

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AskQuestionUiState(
    val isSaving: Boolean = false,
    val questionsRemaining: Int = -1,
    val maxQuestions: Int = -1,
    val creatorName: String = "Ton proche",
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AskQuestionViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AskQuestionUiState())
    val uiState: StateFlow<AskQuestionUiState> = _uiState.asStateFlow()

    private var creatorPublicKey: String? = null

    fun loadData(creatorId: String, recipientId: String) {
        viewModelScope.launch {
            try {
                val creatorDoc = db.collection("users").document(creatorId).get().await()
                val name = creatorDoc.getString("displayName") ?: "Ton proche"
                creatorPublicKey = creatorDoc.getString("publicEncryptionKey")

                val recipientDoc = db.collection("users").document(creatorId)
                    .collection("recipients").document(recipientId).get().await()
                
                val asked = recipientDoc.getLong("questionsAskedCount")?.toInt() ?: 0
                val max = recipientDoc.getLong("maxQuestionsAllowed")?.toInt()
                
                _uiState.value = _uiState.value.copy(
                    creatorName = name,
                    maxQuestions = max ?: -1,
                    questionsRemaining = if (max != null) max - asked else -1
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun sealQuestion(creatorId: String, recipientId: String, questionText: String) {
        val publicKey = creatorPublicKey
        if (publicKey == null) {
            _uiState.value = _uiState.value.copy(error = "Clé publique du créateur manquante")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                // Chiffrement avec la clé publique du Créateur (RSA-OAEP)
                val encryptedQuestion = encryptionManager.encryptWithPublicKey(
                    questionText,
                    Base64.decode(publicKey, Base64.DEFAULT)
                )

                val encryptedBase64 = Base64.encodeToString(
                    encryptedQuestion,
                    Base64.DEFAULT
                )

                val data = hashMapOf(
                    "creatorId" to creatorId,
                    "recipientId" to recipientId,
                    "questionText" to encryptedBase64
                )
                
                functions.getHttpsCallable("sealPendingQuestion")
                    .call(data)
                    .await()
                
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
