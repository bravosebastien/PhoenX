package com.example.phoenx.ui.screens.fil

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.ai.OnDeviceAIManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.data.sync.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val onDeviceAIManager: OnDeviceAIManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _entryId = MutableStateFlow<String?>(null)

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val entry: StateFlow<OfflineEntry?> = _entryId
        .filterNotNull()
        .flatMapLatest { id -> offlineEntryDao.getEntryById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val complements: StateFlow<List<OfflineEntry>> = _entryId
        .filterNotNull()
        .flatMapLatest { id -> offlineEntryDao.getComplements(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decryptedContent: StateFlow<String> = entry
        .map { it?.let { encryptionManager.decryptText(it.encryptedPayload) } ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val recipients: StateFlow<List<RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadEntry(id: String) {
        _entryId.value = id
    }

    fun clearError() {
        _error.value = null
    }

    fun updateContent(newText: String) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            try {
                val encrypted = encryptionManager.encryptText(newText)
                offlineEntryDao.updateEntryContent(encrypted, id)
                android.util.Log.d("MemoryDetailDebug", "Contenu mis à jour en local pour id=$id, taille chiffrée=${encrypted.size}")
                
                // Régénération de l'analyse IA locale après modification
                val analysis = onDeviceAIManager.analyzeLocally(newText)
                offlineEntryDao.updateEntrySummary(analysis.summary, id)
                android.util.Log.d("MemoryDetailDebug", "Résumé IA mis à jour : ${analysis.summary}")

                triggerSync(id)
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Error updating content", e)
            }
        }
    }

    fun updateRecipients(newRecipientIds: List<String>) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryRecipients(newRecipientIds.joinToString(","), id)
            triggerSync(id)
        }
    }

    fun updateVisibility(visibility: String) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryVisibility(visibility, id)
            triggerSync(id)
        }
    }

    fun updateCompartments(selectedIds: List<String>) {
        val id = _entryId.value ?: return
        // Format CSV : ,ID1,ID2,
        val csv = if (selectedIds.isEmpty()) "" else ",${selectedIds.joinToString(",")},"
        viewModelScope.launch {
            offlineEntryDao.updateEntryCompartments(csv, id)
            triggerSync(id)
        }
    }

    fun updateCategory(category: String) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryCategory(category, id)
            triggerSync(id)
        }
    }

    fun updateMemoryDate(date: Long?) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryMemoryDate(date, id)
            triggerSync(id)
        }
    }

    fun updateMemoryPeriod(start: Long?, end: Long?) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryMemoryPeriod(start, end, id)
            triggerSync(id)
        }
    }

    fun updateLocation(lat: Double?, lng: Double?, name: String?, locId: String?) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryLocation(lat, lng, name, locId, id)
            triggerSync(id)
        }
    }

    /**
     * Récupère les détails d'un lieu Firestore et les assigne au souvenir local.
     */
    fun assignLocationFromId(locationId: String) {
        val uid = auth.currentUser?.uid ?: return
        val id = _entryId.value ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid)
                    .collection("locations").document(locationId).get().await()
                
                val lat = doc.getDouble("latitude")
                val lng = doc.getDouble("longitude")
                val name = doc.getString("placeName")
                
                updateLocation(lat, lng, name, locationId)
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Erreur résolution lieu Firestore", e)
            }
        }
    }

    fun deleteMemory() {
        val id = _entryId.value ?: return
        deleteEntryById(id, isParent = true)
    }

    fun deleteComplement(complementId: String) {
        deleteEntryById(complementId, isParent = false)
    }

    private fun deleteEntryById(id: String, isParent: Boolean) {
        val uid = auth.currentUser?.uid ?: run {
            _error.value = "Utilisateur non connecté"
            return
        }

        viewModelScope.launch {
            try {
                // 1. Suppression Firestore d'abord (Sécurité)
                db.collection("users").document(uid)
                    .collection("entries").document(id)
                    .delete()
                    .await()
                
                android.util.Log.d("MemoryDetailDebug", "Document Firestore supprimé pour id=$id")

                // 2. Suppression Room seulement après succès Cloud
                offlineEntryDao.deleteEntry(id)
                android.util.Log.d("MemoryDetailDebug", "Entrée locale supprimée pour id=$id")

                if (isParent) {
                    _deleteSuccess.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Erreur lors de la suppression de $id", e)
                _error.value = "Échec de la suppression : ${e.message}"
            }
        }
    }

    private suspend fun triggerSync(entryId: String) {
        // Passage en pending pour forcer le Worker à le voir
        offlineEntryDao.updateSyncStatus(entryId, "pending")
        android.util.Log.d("MemoryDetailDebug", "syncStatus repassé à pending pour id=$entryId")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
        android.util.Log.d("MemoryDetailDebug", "OneTimeWorkRequest enqueue pour id=$entryId")
    }
}
