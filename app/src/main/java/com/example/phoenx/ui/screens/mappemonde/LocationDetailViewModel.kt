package com.example.phoenx.ui.screens.mappemonde

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.sync.toOfflineEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class LocationDetailUiState {
    object Loading : LocationDetailUiState()
    data class Success(val location: LocationMemory, val entries: List<OfflineEntry>) : LocationDetailUiState()
    object Error : LocationDetailUiState()
}

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: com.example.phoenx.data.encryption.EncryptionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationDetailUiState>(LocationDetailUiState.Loading)
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    private val _recipients = MutableStateFlow<List<com.example.phoenx.data.local.RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = _recipients.asStateFlow()

    init {
        loadRecipients()
    }

    private fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collect { list ->
                _recipients.value = list
            }
        }
    }

    fun loadLocationData(locationId: String, targetCreatorId: String? = null) {
        val currentUid = auth.currentUser?.uid ?: return
        val userId = targetCreatorId ?: currentUid
        android.util.Log.d("PHOENX_MAP_TRACE", "loadLocationData [START]: locationId=$locationId, targetCreatorId=$targetCreatorId, finalUserId=$userId")
        
        viewModelScope.launch {
            try {
                // Charger le lieu depuis Firestore
                android.util.Log.d("PHOENX_MAP_TRACE", "Firestore query: users/$userId/locations/$locationId")
                val doc = db.collection("users").document(userId)
                    .collection("locations").document(locationId).get().await()
                
                if (!doc.exists()) {
                    android.util.Log.e("PHOENX_MAP_TRACE", "Firestore result: Document NOT FOUND for $locationId at path users/$userId/locations/")
                    _uiState.value = LocationDetailUiState.Error
                    return@launch
                }

                val location = doc.toObject(LocationMemory::class.java)?.copy(id = doc.id)
                
                if (location != null) {
                    android.util.Log.d("PHOENX_MAP_TRACE", "Firestore success: ${location.placeName}, linked entryIds=${location.entryIds}")
                    
                    // Si on est héritier, il faut lire les entries sur Firestore (v9.4.27)
                    if (targetCreatorId != null && targetCreatorId != currentUid) {
                        android.util.Log.d("PHOENX_MAP_TRACE", "Mode Héritier: Recherche des souvenirs liés à locationId=$locationId sur Firestore")
                        
                        // v9.4.27 Fix Point 2 & 3: Requête sur les entries filtrée par locationId ET recipientIds
                        // On doit fusionner Public (EVERYONE) et Privé (recipientIds array-contains)
                        val publicEntries = db.collection("users").document(targetCreatorId)
                            .collection("entries")
                            .whereEqualTo("locationId", locationId)
                            .whereEqualTo("visibility", "EVERYONE")
                            .get().await()

                        val privateEntries = db.collection("users").document(targetCreatorId)
                            .collection("entries")
                            .whereEqualTo("locationId", locationId)
                            .whereArrayContains("recipientIds", currentUid)
                            .get().await()

                        val snaps = (publicEntries.documents + privateEntries.documents).distinctBy { it.id }
                        android.util.Log.d("PHOENX_MAP_TRACE", "Mode Héritier: ${snaps.size} souvenirs filtrés trouvés.")

                        // Récupération de la clé miroir pour déchiffrer les titres
                        val keyDoc = db.collection("users").document(targetCreatorId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        val key = keyBase64?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }

                        val memories = snaps.mapNotNull { it.toOfflineEntry(encryptionManager, key) }
                        memories.forEach { m ->
                           android.util.Log.d("PHOENX_MAP_TRACE", " - Souvenir récupéré: ID=${m.id}, Title=${m.aiSummary}")
                        }

                        _uiState.value = LocationDetailUiState.Success(location, memories)
                    } else {
                        // Mode Créateur : Flux temps réel local
                        android.util.Log.d("PHOENX_MAP_TRACE", "Mode Créateur: Lancement collection local Flow pour $locationId")
                        offlineEntryDao.getEntriesForLocation(locationId).collectLatest { relatedEntries ->
                            android.util.Log.d("PHOENX_MAP_TRACE", "Local Flow update: ${relatedEntries.size} entries found.")
                            // Fallback : si aucun via locationId, on cherche par nom ou pactId (legacy)
                            val finalEntries = relatedEntries.ifEmpty {
                                android.util.Log.d("PHOENX_MAP_TRACE", "No entries via locationId, trying legacy fallback...")
                                val all = withContext(Dispatchers.IO) {
                                    offlineEntryDao.getAllEntriesSync()
                                }
                                all.filter { (it.locationName == location.placeName) || (it.pactId == location.id) }
                            }
                            _uiState.value = LocationDetailUiState.Success(location, finalEntries)
                        }
                    }
                } else {
                    android.util.Log.e("PHOENX_MAP_TRACE", "Firestore error: doc.toObject returned NULL for $locationId")
                }
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_MAP_TRACE", "EXCEPTION critique in loadLocationData($locationId): ${e.message}", e)
                _uiState.value = LocationDetailUiState.Error
            }
        }
    }

    fun updateLocation(locationId: String, newName: String, emoji: String, start: Long?, end: Long?) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("locations").document(locationId)
                    .update(mapOf(
                        "placeName" to newName,
                        "emoji" to emoji,
                        "startDate" to start,
                        "endDate" to end,
                        "visitedAt" to (start ?: 0L)
                    )).await()
                loadLocationData(locationId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating location", e)
            }
        }
    }

    fun updateEntrySummary(entryId: String, newSummary: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.updateEntrySummary(newSummary, entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating entry", e)
            }
        }
    }

    fun updateEntryRecipients(entryId: String, newRecipientDocIds: List<String>) {
        viewModelScope.launch {
            try {
                // v9.2.2 : Remappage DocID -> UID pour la sécurité Firestore
                val persistentIds = newRecipientDocIds.map { docId ->
                    recipients.value.find { it.id == docId }?.linkedUid ?: docId
                }
                offlineEntryDao.updateEntryRecipients(persistentIds.joinToString(","), entryId)
                
                // Force sync
                offlineEntryDao.updateSyncStatus(entryId, "pending")
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.phoenx.data.sync.SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueue(syncRequest)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating recipients", e)
            }
        }
    }

    fun updateEntryVisibility(entryId: String, visibility: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.updateEntryVisibility(visibility, entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating visibility", e)
            }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.deleteEntry(entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error deleting entry", e)
            }
        }
    }

    fun detachEntry(entryId: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.detachEntryFromLocation(entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error detaching entry", e)
            }
        }
    }
}
