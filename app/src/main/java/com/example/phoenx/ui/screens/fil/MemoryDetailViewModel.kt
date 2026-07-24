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
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val onDeviceAIManager: OnDeviceAIManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _entryId = MutableStateFlow<String?>(null)
    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    private val _isProtocolActivated = MutableStateFlow(true)
    val isProtocolActivated: StateFlow<Boolean> = _isProtocolActivated.asStateFlow()

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

    /**
     * Retourne la liste des compléments texte DÉCHIFFRÉS (v8.4)
     */
    val decryptedTextComplements: StateFlow<List<Pair<String, String>>> = combine(complements, _heirKey, _isProtocolActivated) { list, key, activated ->
        if (!activated && key != null) {
            return@combine list.filter { it.entryType == "TEXT" || it.entryType == "THOUGHT" }
                .map { it.id to "Souvenir scellé" }
        }
        list.filter { (it.entryType == "TEXT") || (it.entryType == "THOUGHT") }
            .map { it.id to encryptionManager.decryptText(it.encryptedPayload, key) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decryptedContent: StateFlow<String> = combine(entry, _heirKey, _isProtocolActivated) { ent, key, activated ->
        if (!activated && key != null) return@combine "Souvenir scellé"
        ent?.let { encryptionManager.decryptText(it.encryptedPayload, key) } ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /**
     * Modèle structuré pour les items du Portrait (v8.5.7)
     */
    data class PortraitItem(val id: String?, val question: String, val answer: String)

    /**
     * Fusionne le contenu legacy et les nouveaux compléments atomiques (v8.5.7)
     */
    val structuredPortrait: StateFlow<List<PortraitItem>> = combine(decryptedContent, complements, _heirKey, _isProtocolActivated) { content, compList, key, activated ->
        val list = mutableListOf<PortraitItem>()
        
        if (!activated && key != null) {
            // Uniquement les compléments atomiques pour les titres, mais contenu scellé
            compList.filter { it.parentEntryId == _entryId.value && it.entryType == "TEXT" }.forEach { comp ->
                list.add(PortraitItem(comp.id, comp.aiSummary, "Souvenir scellé"))
            }
            return@combine list
        }

        // 1. Parsing Legacy (v8.5.9 - Titres intelligents)
        // NOTE : Il s'agit d'un COMPROMIS pour données manquantes (anciens portraits). 
        // On ne peut pas reconstruire la question réelle, donc on utilise le début du texte comme titre de bandeau.
        if (content.isNotBlank()) {
            if (content.startsWith("[")) {
                try {
                    val arr = JSONArray(content)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val q = obj.getString("q")
                        val a = obj.getString("a")
                        // Si pas de question, on prend le début de la réponse pour le titre
                        val displayQ = q.ifBlank { if (a.length > 30) a.take(30) + "..." else a }
                        list.add(PortraitItem(null, displayQ, a))
                    }
                } catch (e: Exception) {
                    content.split("\n\n").forEach { 
                        val title = if (it.length > 30) it.take(30) + "..." else it
                        list.add(PortraitItem(null, title, it)) 
                    }
                }
            } else {
                content.split("\n\n").forEach { 
                    val title = if (it.length > 30) it.take(30) + "..." else it
                    list.add(PortraitItem(null, title, it)) 
                }
            }
        }
        
        // 2. Standard Atomique (Compléments liés)
        compList.filter { it.parentEntryId == _entryId.value && it.entryType == "TEXT" }.forEach { comp ->
            val decrypted = encryptionManager.decryptText(comp.encryptedPayload, key)
            if (list.none { it.answer == decrypted && it.question == comp.aiSummary }) {
                list.add(PortraitItem(comp.id, comp.aiSummary, decrypted))
            }
        }
        
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipients: StateFlow<List<RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadEntry(id: String, creatorId: String? = null) {
        _entryId.value = id
        if (creatorId != null && creatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                try {
                    // Check protocol status via Cloud Function (v8.5.9)
                    val result = functions.getHttpsCallable("getCreatorProtocolStatus")
                        .call(mapOf("creatorId" to creatorId)).await()
                    
                    val data = result.data as? Map<*, *>
                    _isProtocolActivated.value = data?.get("isActivated") as? Boolean ?: false

                    if (_isProtocolActivated.value) {
                        val keyDoc = db.collection("users").document(creatorId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MemoryDetailVM", "Erreur chargement clé héritage", e)
                    _isProtocolActivated.value = false
                }
            }
        } else {
            _isProtocolActivated.value = true
            _heirKey.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateContent(newText: String) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            try {
                val currentEntry = entry.value ?: return@launch
                val encrypted = encryptionManager.encryptText(newText)
                offlineEntryDao.updateEntryContent(encrypted, id)
                
                // On ne met à jour le résumé que pour les souvenirs "racines" (Étincelles)
                // Les réponses au portrait et aux questions gardent leur titre (la question) (v8.5.9)
                if (currentEntry.parentEntryId == null && currentEntry.entryType != "QUESTION_ANSWER") {
                    val analysis = onDeviceAIManager.analyzeLocally(newText)
                    offlineEntryDao.updateEntrySummary(analysis.summary, id)
                }

                triggerSync(id)
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Error updating content", e)
            }
        }
    }

    fun updateTitle(newTitle: String) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            try {
                offlineEntryDao.updateEntrySummary(newTitle, id)
                triggerSync(id)
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Error updating title", e)
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

    fun updateSilentAttribution(silent: Boolean) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntrySilentAttribution(silent, id)
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
