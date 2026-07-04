package com.example.phoenx.ui.screens.recipient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RecipientViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipientUiState>(RecipientUiState.Loading)
    val uiState: StateFlow<RecipientUiState> = _uiState

    init {
        loadRecipients()
    }

    private fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collectLatest { recipients ->
                _uiState.value = RecipientUiState.Success(recipients)
            }
        }
    }

    fun addRecipient(name: String, email: String, relationship: String) {
        viewModelScope.launch {
            val recipient = RecipientEntity(
                name = name,
                email = email,
                relationship = relationship
            )
            offlineEntryDao.insertRecipient(recipient)
        }
    }

    fun deleteRecipient(recipient: RecipientEntity) {
        viewModelScope.launch {
            offlineEntryDao.deleteRecipient(recipient)
        }
    }

    fun updatePermissions(recipientId: String, canAsk: Boolean, maxQuestions: Int?) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val recipients = (uiState.value as? RecipientUiState.Success)?.recipients ?: return@launch
            val recipient = recipients.find { it.id == recipientId } ?: return@launch
            
            val updated = recipient.copy(
                canAskQuestions = canAsk,
                maxQuestionsAllowed = maxQuestions
            )
            offlineEntryDao.insertRecipient(updated)

            try {
                // Sync with Firestore
                db.collection("users").document(currentUserId)
                    .collection("recipients").document(recipientId)
                    .update(mapOf(
                        "canAskQuestions" to canAsk,
                        "maxQuestionsAllowed" to maxQuestions
                    )).await()

                // Trigger email if activated for the first time
                if (canAsk && !recipient.canAskQuestions) {
                    val userDoc = db.collection("users").document(currentUserId).get().await()
                    val creatorName = userDoc.getString("displayName") ?: "Ton proche"
                    
                    val inviteLink = "https://phoenx.app/ask?creator=$currentUserId&recipient=$recipientId"
                    
                    val data = hashMapOf(
                        "recipientEmail" to recipient.email,
                        "recipientName" to recipient.name,
                        "creatorName" to creatorName,
                        "inviteLink" to inviteLink
                    )
                    functions.getHttpsCallable("notifyQuestionRightGranted").call(data).await()
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Error updating permissions", e)
            }
        }
    }
}

sealed class RecipientUiState {
    object Loading : RecipientUiState()
    data class Success(val recipients: List<RecipientEntity>) : RecipientUiState()
}
