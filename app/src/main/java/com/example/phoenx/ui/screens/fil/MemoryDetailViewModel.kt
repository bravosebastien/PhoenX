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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val onDeviceAIManager: OnDeviceAIManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _entryId = MutableStateFlow<String?>(null)

    val entry: StateFlow<OfflineEntry?> = _entryId
        .filterNotNull()
        .flatMapLatest { id -> offlineEntryDao.getEntryById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val decryptedContent: StateFlow<String> = entry
        .map { it?.let { encryptionManager.decryptText(it.encryptedPayload) } ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val recipients: StateFlow<List<RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadEntry(id: String) {
        _entryId.value = id
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
