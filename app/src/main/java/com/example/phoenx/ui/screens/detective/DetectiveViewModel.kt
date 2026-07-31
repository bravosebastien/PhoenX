package com.example.phoenx.ui.screens.detective

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.EnigmaUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.phoenx.data.sync.toOfflineEntry

data class DetectiveUiState(
    val lockedEntries: List<OfflineEntry> = emptyList(),
    val unlockedEntryId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val daysSinceActivation: Int = 0,
    val creatorName: String = "Ton proche",
    val attempts: Map<String, Int> = emptyMap() // entryId -> count
)

@HiltViewModel
class DetectiveViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectiveUiState())
    val uiState: StateFlow<DetectiveUiState> = _uiState.asStateFlow()

    private val _heirKey = MutableStateFlow<ByteArray?>(null)

    fun loadData(creatorId: String?) {
        val currentUserId = auth.currentUser?.uid ?: return
        val targetCreatorId = creatorId ?: ""
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Charger le nom du créateur et l'activation
                if (targetCreatorId.isNotEmpty() && targetCreatorId != currentUserId) {
                    val creatorDoc = db.collection("users").document(targetCreatorId).get().await()
                    val name = creatorDoc.getString("displayName") ?: "Ton proche"
                    
                    // Trouver le protocole activé pour ce créateur
                    val protocolSnap = db.collection("activationProtocols")
                        .whereEqualTo("creatorId", targetCreatorId)
                        .get().await()
                    
                    val protocolDoc = protocolSnap.documents.find { it.getString("status") == "activated" }
                    val isActivated = protocolDoc != null
                    
                    // v9.4.13 : Chargement de la clé miroir (si activé)
                    if (isActivated) {
                        val keyDoc = db.collection("users").document(targetCreatorId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                        }
                    }

                    // Fallback sur l'heure actuelle si aucun protocole (utile pour les tests)
                    val confirmedAtMillis = protocolDoc?.getTimestamp("confirmedAt")?.toDate()?.time 
                        ?: System.currentTimeMillis()
                    
                    val diff = System.currentTimeMillis() - confirmedAtMillis
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                    
                    _uiState.update { it.copy(creatorName = name, daysSinceActivation = days) }

                    // 2. Charger les énigmes (v8.6.2 Fix: Filtre par enigmaQuestion != null)
                    val snapshot = db.collection("users").document(targetCreatorId)
                        .collection("entries")
                        .whereNotEqualTo("enigmaQuestion", null)
                        .get().await()
                    
                    // v9.4.13 : Passage de la clé héritier
                    val entries = snapshot.documents.mapNotNull { it.toOfflineEntry(encryptionManager, _heirKey.value) }
                    _uiState.update { it.copy(lockedEntries = entries, isLoading = false) }
                } else {
                    // Mode Créateur lui-même (test local)
                    offlineEntryDao.getAllEntries().collect { entries ->
                        val locked = entries.filter { it.enigmaQuestion != null }
                        _uiState.update { it.copy(lockedEntries = locked, isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erreur de connexion") }
            }
        }
    }

    fun attemptUnlock(entry: OfflineEntry, answer: String) {
        val hashedInput = EnigmaUtils.hashAnswer(answer)
        
        if (entry.enigmaAnswer == hashedInput || entry.fallbackAnswer == hashedInput) {
            _uiState.update { it.copy(unlockedEntryId = entry.id) }
        } else {
            val newAttempts = _uiState.value.attempts.toMutableMap()
            val count = (newAttempts[entry.id] ?: 0) + 1
            newAttempts[entry.id] = count
            _uiState.update { it.copy(error = "Mauvaise réponse. Cherche encore...", attempts = newAttempts) }
        }
    }

    fun markAsAutoUnlocked(creatorId: String, entryId: String) {
        viewModelScope.launch {
            try {
                val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
                functions.getHttpsCallable("markEntryAutoUnlocked")
                    .call(mapOf("creatorId" to creatorId, "entryId" to entryId))
                    .await()
                
                // Mettre à jour l'UI locale
                _uiState.update { it.copy(unlockedEntryId = entryId) }
                
            } catch (e: Exception) {
                android.util.Log.e("DetectiveVM", "Erreur lors du déverrouillage automatique", e)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
