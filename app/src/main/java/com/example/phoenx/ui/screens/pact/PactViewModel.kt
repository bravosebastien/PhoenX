package com.example.phoenx.ui.screens.pact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PactEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PactViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PactUiState())
    val uiState: StateFlow<PactUiState> = _uiState.asStateFlow()

    init {
        loadPacts()
    }

    private fun loadPacts() {
        val userId = auth.currentUser?.uid ?: return
        
        // Priorité locale + Sync Firestore
        offlineEntryDao.getAllPacts()
            .onEach { pacts ->
                _uiState.update { it.copy(pacts = pacts, isLoading = false) }
            }
            .launchIn(viewModelScope)

        // Rafraîchir depuis Firestore
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId).collection("pacts").get().await()
                snapshot.documents.forEach { doc ->
                    val pact = PactEntity(
                        id = doc.id,
                        partnerName = doc.getString("partnerName") ?: "",
                        partnerEmail = doc.getString("partnerEmail") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                    offlineEntryDao.insertPact(pact)
                }
            } catch (e: Exception) {
                android.util.Log.e("PactVM", "Error syncing pacts", e)
            }
        }
    }

    fun invitePartner(name: String, email: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val pactData = hashMapOf(
                    "partnerName" to name,
                    "partnerEmail" to email,
                    "status" to "pending",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                // 1. Sauvegarde Firestore (Source de vérité)
                val docRef = db.collection("users").document(userId).collection("pacts").add(pactData).await()
                
                // 2. Sauvegarde Room (Cache local)
                val pact = PactEntity(
                    id = docRef.id,
                    partnerName = name,
                    partnerEmail = email,
                    status = "pending"
                )
                offlineEntryDao.insertPact(pact)
            } catch (e: Exception) {
                android.util.Log.e("PactVM", "Error creating pact", e)
            }
        }
    }

    fun getEntriesForPact(pactId: String): Flow<List<OfflineEntry>> {
        return offlineEntryDao.getEntriesForPact(pactId)
    }
}

data class PactUiState(
    val pacts: List<PactEntity> = emptyList(),
    val isLoading: Boolean = true
)
