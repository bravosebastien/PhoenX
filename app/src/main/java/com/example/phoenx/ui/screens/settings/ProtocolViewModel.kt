package com.example.phoenx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.R
import com.example.phoenx.data.local.DepositaryEntity
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.media.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val mediaManager: MediaManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtocolUiState())
    val uiState: StateFlow<ProtocolUiState> = _uiState.asStateFlow()

    private val _inviteToken = MutableStateFlow<String?>(null)
    val inviteToken: StateFlow<String?> = _inviteToken

    init {
        loadData()
    }

    private fun loadData() {
        val userId = auth.currentUser?.uid ?: return

        // 1. Écoute locale
        offlineEntryDao.getAllDepositaries().onEach { list ->
            _uiState.update { it.copy(depositaries = list) }
        }.launchIn(viewModelScope)

        // 2. Synchronisation Firestore
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val threshold = userDoc.getLong("silenceConfig.thresholdHours")?.toInt() ?: 72
                _uiState.update { it.copy(thresholdHours = threshold) }

                val snapshot = db.collection("users").document(userId)
                    .collection("depositaries").get().await()
                
                val remoteDeps = snapshot.documents.map { doc ->
                    DepositaryEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone"),
                        role = doc.getString("role") ?: "primary",
                        status = doc.getString("status") ?: "invited",
                        photoUrl = doc.getString("photoUrl"),
                        linkedUid = doc.getString("depositaryUid") // v9.1 : Correction nom de champ Firestore
                    )
                }

                offlineEntryDao.clearDepositaries()
                remoteDeps.forEach { offlineEntryDao.insertDepositary(it) }

            } catch (e: Exception) {
                android.util.Log.e("ProtocolVM", "Erreur sync : ${e.message}")
            }
        }
    }

    fun updateThreshold(hours: Int) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(thresholdHours = hours) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .set(mapOf("silenceConfig" to mapOf("thresholdHours" to hours)), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.protocol_error_update_threshold)) }
            }
        }
    }

    fun inviteDepositary(name: String, email: String, role: String, imageUri: android.net.Uri? = null) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                var finalPhotoUrl: String? = null
                
                // 1. Upload photo if present
                if (imageUri != null) {
                    try {
                        val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                            .child("users/$userId/depositaries/$role.jpg")
                        ref.putFile(imageUri).await()
                        finalPhotoUrl = ref.path // Stockage du CHEMIN (v9.4.17)
                    } catch (e: Exception) {
                        android.util.Log.e("ProtocolVM", "Erreur upload photo dépositaire", e)
                    }
                }

                val depositaryId = role
                
                // Firestore first to ensure rule validation
                db.collection("users").document(userId)
                    .collection("depositaries").document(depositaryId)
                    .set(mapOf(
                        "name" to name,
                        "email" to email,
                        "role" to role,
                        "status" to "invited",
                        "photoUrl" to finalPhotoUrl,
                        "createdAt" to System.currentTimeMillis()
                    )).await()

                // Invitation
                val inviteData = hashMapOf(
                    "email" to email,
                    "role" to "depositary",
                    "sourceId" to depositaryId,
                    "label" to if (role == "primary") context.getString(R.string.protocol_invitation_label_primary) else context.getString(R.string.protocol_invitation_label_secondary)
                )
                val result = functions.getHttpsCallable("generateUniversalInvitation").call(inviteData).await()
                val tokenId = (result.data as Map<*, *>)["tokenId"] as String
                
                _inviteToken.value = tokenId

                // Email via Cloud Function (v9.4.10)
                val creatorName = db.collection("users").document(userId).get().await().getString("displayName") ?: context.getString(R.string.recipient_viewmodel_creator_fallback)
                val emailData = hashMapOf(
                    "to" to email,
                    "subject" to context.getString(R.string.protocol_email_subject, creatorName),
                    "text" to context.getString(R.string.protocol_email_text, name, creatorName, tokenId)
                )
                functions.getHttpsCallable("sendMail").call(emailData).await()

                // Sync local
                offlineEntryDao.insertDepositary(DepositaryEntity(
                    id = depositaryId,
                    name = name,
                    email = email,
                    role = role,
                    status = "invited",
                    photoUrl = finalPhotoUrl
                ))

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun removeDepositary(id: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("depositaries").document(id)
                    .delete().await()
                offlineEntryDao.deleteDepositary(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.protocol_error_remove)) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSuccess() { _uiState.update { it.copy(isSuccess = false) } }
}

data class ProtocolUiState(
    val depositaries: List<DepositaryEntity> = emptyList(),
    val thresholdHours: Int = 72,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
