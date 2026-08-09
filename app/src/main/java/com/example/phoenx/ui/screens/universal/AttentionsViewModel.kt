package com.example.phoenx.ui.screens.universal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LivingLinkUiModel(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val type: String,
    val sentAt: Long,
    val decryptedText: String?,
    val mediaUrls: List<String> = emptyList()
)

data class AttentionsUiState(
    val isLoading: Boolean = true,
    val links: List<LivingLinkUiModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AttentionsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttentionsUiState())
    val uiState: StateFlow<AttentionsUiState> = _uiState.asStateFlow()

    init {
        loadAttentions()
    }

    fun loadAttentions() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Récupération des Liens Vivants envoyés à l'utilisateur
                val snapshot = db.collection("livingLinks")
                    .whereEqualTo("recipientId", userId)
                    .whereEqualTo("status", "sent")
                    .get().await()

                val results = snapshot.documents.mapNotNull { doc ->
                    try {
                        val creatorId = doc.getString("creatorId") ?: ""
                        
                        // Récupération du nom du créateur (Dénormalisation ou requête)
                        val creatorDoc = db.collection("users").document(creatorId).get().await()
                        val creatorName = creatorDoc.getString("displayName") ?: "Un proche"

                        // 2. Déchiffrement de la LinkKey (RSA)
                        val encryptedKeyBlob = doc.getBlob("encryptedLinkKey")?.toBytes() ?: return@mapNotNull null
                        val decryptedKeyBase64 = encryptionManager.decryptWithPrivateKey(encryptedKeyBlob)
                        val linkKey = android.util.Base64.decode(decryptedKeyBase64, android.util.Base64.DEFAULT)

                        // 3. Déchiffrement du contenu (AES)
                        val encryptedContentBlob = doc.getBlob("encryptedContent")?.toBytes() ?: return@mapNotNull null
                        val decryptedText = encryptionManager.decryptText(encryptedContentBlob, linkKey)

                        LivingLinkUiModel(
                            id = doc.id,
                            creatorId = creatorId,
                            creatorName = creatorName,
                            type = doc.getString("type") ?: "TEXT",
                            sentAt = doc.getTimestamp("sentAt")?.toDate()?.time ?: 0L,
                            decryptedText = decryptedText,
                            mediaUrls = (doc.get("mediaUrls") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("AttentionsVM", "Erreur déchiffrement lien ${doc.id}", e)
                        null
                    }
                }.sortedByDescending { it.sentAt }

                _uiState.update { it.copy(links = results, isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("AttentionsVM", "Erreur chargement attentions", e)
                _uiState.update { it.copy(isLoading = false, error = "Impossible de charger les attentions.") }
            }
        }
    }
}
