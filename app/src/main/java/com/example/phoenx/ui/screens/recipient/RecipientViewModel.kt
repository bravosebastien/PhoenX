package com.example.phoenx.ui.screens.recipient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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

    fun getEntriesForRecipient(recipientId: String): Flow<List<OfflineEntry>> = 
        offlineEntryDao.getEntriesForRecipient(recipientId)

    fun getEntriesForRecipientUnified(recipientId: String): Flow<List<OfflineEntry>> = 
        offlineEntryDao.getEntriesForRecipientUnified(recipientId)

    fun getPortraitForRecipient(recipientId: String): Flow<OfflineEntry?> = 
        offlineEntryDao.getPortraitEntryForRecipient(recipientId)

    private fun loadRecipients() {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Écouter les changements locaux (Room)
        viewModelScope.launch {
            try {
                offlineEntryDao.getAllRecipients().collectLatest { recipients ->
                    _uiState.value = RecipientUiState.Success(recipients)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur chargement Room", e)
                _uiState.value = RecipientUiState.Success(emptyList())
            }
        }

        // 2. Synchroniser depuis Firestore
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("recipients").get().await()
                
                snapshot.documents.forEach { doc ->
                    val recipient = RecipientEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        canAskQuestions = doc.getBoolean("canAskQuestions") ?: false,
                        maxQuestionsAllowed = doc.getLong("maxQuestionsAllowed")?.toInt()
                    )
                    offlineEntryDao.insertRecipient(recipient)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur sync Firestore -> Room", e)
            }
        }
    }

    fun addRecipient(name: String, email: String, relationship: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Sauvegarde Firestore
                val recipientData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "relationship" to relationship,
                    "status" to "invited"
                )
                val docRef = db.collection("users").document(userId)
                    .collection("recipients").add(recipientData).await()

                // 2. Room local
                val recipient = RecipientEntity(
                    id = docRef.id,
                    name = name,
                    email = email,
                    relationship = relationship
                )
                offlineEntryDao.insertRecipient(recipient)

                // 3. Invitation Universelle (v7.2)
                val inviteData = hashMapOf(
                    "email" to email,
                    "role" to "recipient",
                    "sourceId" to docRef.id,
                    "label" to "Héritier"
                )
                val result = functions.getHttpsCallable("generateUniversalInvitation").call(inviteData).await()
                val tokenId = (result.data as Map<*, *>)["tokenId"] as String

                // 4. Envoi Email
                val userDoc = db.collection("users").document(userId).get().await()
                val creatorName = userDoc.getString("displayName") ?: "Ton proche"
                
                val emailData = hashMapOf(
                    "to" to email,
                    "message" to hashMapOf(
                        "subject" to "$creatorName t'a choisi comme héritier",
                        "text" to "Lien pour rejoindre son cercle de confiance : https://phoenx.app/join/$tokenId"
                    )
                )
                db.collection("mail").add(emailData).await()

            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur ajout destinataire", e)
            }
        }
    }

    fun deleteRecipient(recipient: RecipientEntity) {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("RecipientVM", "Suppression demandée pour id=${recipient.id}")
        viewModelScope.launch {
            try {
                // 1. Suppression Firestore
                db.collection("users").document(userId)
                    .collection("recipients").document(recipient.id)
                    .delete().await()

                android.util.Log.d("RecipientVM", "Suppression Firestore réussie pour id=${recipient.id}")

                // 2. Suppression Room local
                offlineEntryDao.deleteRecipient(recipient)
            } catch (e: Exception) {
                android.util.Log.e("RecipientVM", "Erreur suppression destinataire", e)
            }
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
