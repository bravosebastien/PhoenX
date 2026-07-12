package com.example.phoenx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.DepositaryEntity
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtocolUiState())
    val uiState: StateFlow<ProtocolUiState> = _uiState

    private val _inviteToken = MutableStateFlow<String?>(null)
    val inviteToken: StateFlow<String?> = _inviteToken

    private val _secondaryInviteToken = MutableStateFlow<String?>(null)
    val secondaryInviteToken: StateFlow<String?> = _secondaryInviteToken

    private val _shortCode = MutableStateFlow<String?>(null)
    val shortCode: StateFlow<String?> = _shortCode

    private val _secondaryShortCode = MutableStateFlow<String?>(null)
    val secondaryShortCode: StateFlow<String?> = _secondaryShortCode

    init {
        loadDepositaries()
    }

    private fun loadDepositaries() {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Écouter les changements locaux pour l'UI (Réactivité maximale)
        viewModelScope.launch {
            offlineEntryDao.getDepositary().collectLatest { dep ->
                if (dep != null) {
                    _uiState.update { it.copy(
                        name = dep.name,
                        email = dep.email,
                        phone = dep.phone,
                        status = dep.status
                    ) }
                }
            }
        }

        // 2. Synchroniser depuis Firestore (Source de vérité)
        viewModelScope.launch {
            try {
                // Charger la config de silence
                val userDoc = db.collection("users").document(userId).get().await()
                val threshold = userDoc.getLong("silenceConfig.thresholdHours")?.toInt() ?: 72
                _uiState.update { it.copy(thresholdHours = threshold) }

                // Charger le dépositaire primaire
                val primaryDoc = db.collection("users").document(userId)
                    .collection("depositaries").document("primary").get().await()
                
                if (primaryDoc.exists()) {
                    val name = primaryDoc.getString("name") ?: ""
                    val email = primaryDoc.getString("email") ?: ""
                    val phone = primaryDoc.getString("phone") ?: ""
                    val status = primaryDoc.getString("status") ?: "active"

                    // Mettre à jour Room si nécessaire
                    val local = offlineEntryDao.getDepositarySync()
                    if (local == null || local.name != name || local.email != email) {
                        offlineEntryDao.clearDepositaries()
                        offlineEntryDao.insertDepositary(DepositaryEntity(
                            name = name, 
                            email = email, 
                            phone = phone,
                            status = status
                        ))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_PROTO", "Erreur sync Firestore -> Room: ${e.message}")
            }
        }

        // 3. Vérifier le secondaire sur Firestore
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId)
                    .collection("depositaries").document("secondary").get().await()
                
                if (doc.exists()) {
                    _uiState.update { it.copy(
                        hasSecondaryDepositary = true,
                        secondaryName = doc.getString("name") ?: "",
                        secondaryEmail = doc.getString("email") ?: "",
                        secondaryPhone = doc.getString("phone") ?: ""
                    ) }
                }
            } catch (e: Exception) {
                // Erreur silencieuse
            }
        }
    }

    fun saveProtocol(name: String, email: String, phone: String, threshold: Int) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }

        viewModelScope.launch {
            try {
                // 1. Sauvegarde locale
                offlineEntryDao.clearDepositaries()
                val dep = DepositaryEntity(name = name, email = email, phone = phone)
                offlineEntryDao.insertDepositary(dep)

                // 2. Sauvegarde Firestore
                val depositaryId = "primary"
                val depositaryData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "role" to "primary",
                    "status" to "active"
                )
                db.collection("users").document(userId)
                    .collection("depositaries").document(depositaryId)
                    .set(depositaryData).await()

                // Enregistrement du délai de contestation (Seuil de sécurité)
                db.collection("users").document(userId)
                    .set(
                        mapOf("silenceConfig" to mapOf("thresholdHours" to threshold)),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()

                // 3. Génération du Token Universel (v7.2)
                val inviteData = hashMapOf(
                    "email" to email,
                    "role" to "depositary",
                    "sourceId" to depositaryId,
                    "label" to "Gardien de confiance"
                )
                val result = functions.getHttpsCallable("generateUniversalInvitation").call(inviteData).await()
                val tokenId = (result.data as Map<*, *>)["tokenId"] as String
                
                _inviteToken.value = tokenId
                _shortCode.value = tokenId // On utilise le tokenId comme lien (unification)
                
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                android.util.Log.e("PHOENX_PROTO", "Erreur sauvegarde primaire: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Erreur de sauvegarde") }
            }
        }
    }

    fun loadCreatorStatus(creatorId: String) {
        // ... (Non modifié ici)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Sauvegarde le Dépositaire secondaire (Palier 4 d'alerte).
     */
    fun saveSecondaryDepositary(name: String, email: String, phone: String) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
        viewModelScope.launch {
            try {
                val depositaryId = "secondary"
                val depositaryData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "role" to "secondary",
                    "status" to "active"
                )
                db.collection("users").document(userId)
                    .collection("depositaries").document(depositaryId)
                    .set(depositaryData).await()

                // Génération du token universel pour le secondaire
                val inviteData = hashMapOf(
                    "email" to email,
                    "role" to "depositary",
                    "sourceId" to depositaryId,
                    "label" to "Gardien de confiance"
                )
                val result = functions.getHttpsCallable("generateUniversalInvitation").call(inviteData).await()
                val tokenId = (result.data as Map<*, *>)["tokenId"] as String

                _secondaryInviteToken.value = tokenId
                _secondaryShortCode.value = tokenId
                
                _uiState.update { it.copy(
                    hasSecondaryDepositary = true,
                    secondaryName = name,
                    secondaryEmail = email,
                    secondaryPhone = phone,
                    isLoading = false,
                    isSuccess = true
                ) }

            } catch (e: Exception) {
                android.util.Log.e("PHOENX_PROTO", "Erreur sauvegarde secondaire: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Erreur de sauvegarde") }
            }
        }
    }
}

data class ProtocolUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = "Dormant",
    val thresholdHours: Int = 72,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val hasSecondaryDepositary: Boolean = false,
    val secondaryName: String = "",
    val secondaryEmail: String = "",
    val secondaryPhone: String = ""
)
