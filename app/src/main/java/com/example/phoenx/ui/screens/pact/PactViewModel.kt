package com.example.phoenx.ui.screens.pact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PactEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PactViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(PactUiState())
    val uiState: StateFlow<PactUiState> = _uiState.asStateFlow()

    private val _invitationLink = MutableSharedFlow<String>()
    val invitationLink = _invitationLink.asSharedFlow()

    init {
        loadPacts()
    }

    private fun loadPacts() {
        val userId = auth.currentUser?.uid ?: return
        
        offlineEntryDao.getAllPacts()
            .onEach { pacts ->
                _uiState.update { it.copy(pacts = pacts, isLoading = false) }
            }
            .launchIn(viewModelScope)

        // Sync avec la collection "mirrors" (v9.4.27)
        viewModelScope.launch {
            try {
                // On écoute les miroirs où l'utilisateur est soit A soit B
                val queryA = db.collection("mirrors").whereEqualTo("creatorAId", userId).get().await()
                val queryB = db.collection("mirrors").whereEqualTo("creatorBId", userId).get().await()
                
                (queryA.documents + queryB.documents).distinctBy { it.id }.forEach { doc ->
                    val isA = doc.getString("creatorAId") == userId
                    val partnerId = if (isA) doc.getString("creatorBId") else doc.getString("creatorAId")
                    val partnerName = if (isA) doc.getString("partnerBName") else doc.getString("partnerAName")
                    val partnerEmail = if (isA) doc.getString("creatorBEmail") else doc.getString("creatorAEmail")
                    
                    val myStatus = if (isA) doc.getString("statusA") else doc.getString("statusB")
                    val partnerStatus = if (isA) doc.getString("statusB") else doc.getString("statusA")
                    
                    val myConsent = if (isA) doc.getBoolean("aConsentBook") ?: false else doc.getBoolean("bConsentBook") ?: false
                    val partnerConsent = if (isA) doc.getBoolean("bConsentBook") ?: false else doc.getBoolean("aConsentBook") ?: false

                    val pact = PactEntity(
                        id = doc.id,
                        partnerId = partnerId,
                        partnerName = partnerName ?: "Partenaire",
                        partnerEmail = partnerEmail ?: "",
                        status = doc.getString("status") ?: "pending",
                        myStatus = myStatus ?: "writing",
                        partnerStatus = partnerStatus ?: "writing",
                        myConsentToBook = myConsent,
                        partnerConsentToBook = partnerConsent,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                    offlineEntryDao.insertPact(pact)
                }
            } catch (e: Exception) {
                android.util.Log.e("PactVM", "Error syncing mirrors", e)
            }
        }
    }

    fun invitePartner(name: String, email: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Un proche"
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // 1. Création du miroir dans la collection dédiée
                val mirrorData = hashMapOf(
                    "creatorAId" to userId,
                    "partnerAName" to userName,
                    "creatorAEmail" to (auth.currentUser?.email ?: ""),
                    "creatorBEmail" to email.lowercase(),
                    "partnerBName" to name,
                    "status" to "pending",
                    "statusA" to "writing",
                    "statusB" to "writing",
                    "aConsentBook" to false,
                    "bConsentBook" to false,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                val mirrorDoc = db.collection("mirrors").add(mirrorData).await()
                val mirrorId = mirrorDoc.id

                // 2. Génération de l'invitation universelle (v9.4.27)
                val inviteParams = hashMapOf(
                    "email" to email.lowercase(),
                    "role" to "mirror_partner",
                    "sourceId" to mirrorId,
                    "label" to "Partenaire de Miroir",
                    "expiresHours" to 168
                )
                
                val result = functions.getHttpsCallable("generateUniversalInvitation").call(inviteParams).await()
                val tokenId = (result.data as Map<*, *>)["tokenId"] as String
                
                val inviteLink = "https://phoenx.app/invite?token=$tokenId"
                
                // 3. Sauvegarde locale
                val pact = PactEntity(
                    id = mirrorId,
                    partnerName = name,
                    partnerEmail = email,
                    status = "pending"
                )
                offlineEntryDao.insertPact(pact)
                
                _invitationLink.emit(inviteLink)
                _uiState.update { it.copy(isLoading = false) }
                
            } catch (e: Exception) {
                android.util.Log.e("PactVM", "Error creating mirror", e)
                _uiState.update { it.copy(isLoading = false, error = "Échec de l'invitation.") }
            }
        }
    }

    fun toggleConsentToBook(pactId: String, consent: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("mirrors").document(pactId).get().await()
                val isA = doc.getString("creatorAId") == userId
                val field = if (isA) "aConsentBook" else "bConsentBook"
                
                db.collection("mirrors").document(pactId).update(field, consent).await()
                
                // Update local
                val existing = _uiState.value.pacts.find { it.id == pactId }
                existing?.let {
                    offlineEntryDao.insertPact(it.copy(myConsentToBook = consent))
                }
            } catch (e: Exception) {
                android.util.Log.e("PactVM", "Error updating consent", e)
            }
        }
    }

    fun completeVersion(pactId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("mirrors").document(pactId).get().await()
                val isA = doc.getString("creatorAId") == userId
                val field = if (isA) "statusA" else "statusB"
                
                db.collection("mirrors").document(pactId).update(field, "completed").await()
                
                // Si les deux sont complétés, on passe le statut global à active (révélé)
                val otherStatus = if (isA) doc.getString("statusB") else doc.getString("statusA")
                if (otherStatus == "completed") {
                    db.collection("mirrors").document(pactId).update("status", "active").await()
                }

                // Update local
                val existing = _uiState.value.pacts.find { it.id == pactId }
                existing?.let {
                    offlineEntryDao.insertPact(it.copy(myStatus = "completed"))
                }
            } catch (e: Exception) {
                android.util.Log.e("PactVM", "Error completing version", e)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getEntriesForPact(pactId: String): Flow<List<OfflineEntry>> {
        return offlineEntryDao.getEntriesForPact(pactId)
    }
}

data class PactUiState(
    val pacts: List<PactEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
