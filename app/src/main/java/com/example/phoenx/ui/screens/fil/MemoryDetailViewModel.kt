package com.example.phoenx.ui.screens.fil

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.ai.OnDeviceAIManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.*
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.domain.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import java.util.*
import javax.inject.Inject

import org.json.JSONArray
import com.example.phoenx.data.audio.PhoenXAudioRecorder
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
    private val audioRecorder: com.example.phoenx.data.audio.PhoenXAudioRecorder,
    private val wavRecorder: com.example.phoenx.data.audio.WavAudioRecorder,
    private val sttManager: com.example.phoenx.data.audio.SpeechToTextManager,
    private val mediaManager: com.example.phoenx.data.media.MediaManager, // v9.4.27
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isVoiceNoteOverlayOpen = MutableStateFlow(false) // v9.4.27
    val isVoiceNoteOverlayOpen: StateFlow<Boolean> = _isVoiceNoteOverlayOpen.asStateFlow()

    val sttPartialText = sttManager.partialText

    private var currentAudioFile: File? = null

    fun openVoiceNoteOverlay() {
        _isVoiceNoteOverlayOpen.value = true
    }

    fun closeVoiceNoteOverlay() {
        _isVoiceNoteOverlayOpen.value = false
    }

    fun startAudioRecording() {
        val file = File(context.cacheDir, "temp_complement_${System.currentTimeMillis()}.wav") // Extension .wav
        currentAudioFile = file
        
        android.util.Log.d("VoiceNoteDiag", "Démarrage WavRecorder (Alternative robuste)...")
        wavRecorder.start(file)
        _isRecording.value = true
    }

    fun stopAudioRecording(parentId: String) {
        android.util.Log.d("VoiceNoteDiag", "Arrêt de l'enregistrement demandé")
        _isRecording.value = false
        _isVoiceNoteOverlayOpen.value = false
        
        wavRecorder.stop()
        
        currentAudioFile?.let { file ->
            val size = if (file.exists()) file.length() else -1
            android.util.Log.d("VoiceNoteDiag", "Fichier WAV final: ${file.absolutePath}, Taille: $size octets")
            
            // Diagnostic MediaExtractor (même si c'est du WAV, on vérifie si l'en-tête est valide)
            inspectAudioFile(file)
            
            addMediaComplement(parentId, file, "AUDIO", "Note vocale")
        }
    }

    private fun inspectAudioFile(file: File) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            val trackCount = extractor.trackCount
            android.util.Log.d("VoiceNoteDiag", "DIAGNOSTIC MediaExtractor : $trackCount piste(s) trouvée(s)")
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                android.util.Log.d("VoiceNoteDiag", "Piste #$i : ${format.getString(MediaFormat.KEY_MIME)}")
            }
            extractor.release()
        } catch (e: Exception) {
            android.util.Log.e("VoiceNoteDiag", "ÉCHEC DIAGNOSTIC MediaExtractor : ${e.message}")
        }
    }

    private val _entryId = MutableStateFlow<String?>(null)
    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    private val _isProtocolActivated = MutableStateFlow(true)
    val isProtocolActivated: StateFlow<Boolean> = _isProtocolActivated.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _suggestedPersons = MutableStateFlow<List<SimplifiedPerson>>(emptyList())
    val suggestedPersons: StateFlow<List<SimplifiedPerson>> = _suggestedPersons.asStateFlow()

    val recipients: StateFlow<List<RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entry: StateFlow<OfflineEntry?> = _entryId
        .filterNotNull()
        .flatMapLatest { id -> offlineEntryDao.getEntryById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Source de vérité unique pour les destinataires sélectionnés (remappés en DocIDs pour l'UI)
     */
    val selectedRecipientIds: StateFlow<List<String>> = entry
        .filterNotNull()
        .combine(recipients) { rawEntry, recipientsList ->
            rawEntry.recipientIds.split(",")
                .filter { it.isNotBlank() }
                .map { it.trim() }
                .map { persistentId ->
                    recipientsList.find { it.linkedUid == persistentId }?.id ?: persistentId
                }.distinct()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Source de vérité unique pour les personnes citées (v9.4.26 : Unifiée)
     * Réagit aux changements de personIds dans l'entrée.
     */
    val selectedPersons: StateFlow<List<SimplifiedPerson>> = combine(
        entry.filterNotNull().map { it.personIds }.distinctUntilChanged(),
        offlineEntryDao.getAllPersons(),
        offlineEntryDao.getAllRecipients(),
        offlineEntryDao.getAllWitnesses(),
        offlineEntryDao.getAllDepositaries()
    ) { idsCsv, persons, recipients, witnesses, depositaries ->
        val ids = idsCsv.split(",").filter { it.isNotBlank() }.map { it.trim() }.distinct()
        if (ids.isEmpty()) return@combine emptyList()

        val allSimplified = persons.toSimplified() + 
                        recipients.toSimplifiedRecipient() + 
                        witnesses.toSimplifiedWitness() + 
                        depositaries.toSimplifiedDepositary()
        
        // On inclut aussi "Moi" si présent dans les IDs
        val user = auth.currentUser
        val me = if (user != null && ids.contains("ME_${user.uid}")) {
            listOf(SimplifiedPerson(
                id = "ME_${user.uid}",
                name = user.displayName ?: "Moi",
                photoUrl = user.photoUrl?.toString(),
                sourceType = "auteur",
                relationship = "Auteur",
                isMe = true
            ))
        } else emptyList()

        (allSimplified + me).filter { ids.contains(it.id) }.distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    /**
     * Met à jour le titre d'un complément (v9.4.27)
     */
    fun updateComplementTitle(complementId: String, newTitle: String) {
        viewModelScope.launch {
            offlineEntryDao.updateEntryMediaTitle(newTitle, complementId)
            triggerSync(complementId)
        }
    }

    /**
     * Met à jour le commentaire d'un complément (v9.4.27)
     */
    fun updateComplementComment(complementId: String, newComment: String?) {
        viewModelScope.launch {
            offlineEntryDao.updateEntryComment(newComment, complementId)
            triggerSync(complementId)
        }
    }

    /**
     * Chiffre et uploade une photo de couverture pour un complément (v9.4.27)
     */
    fun updateComplementCover(complementId: String, imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val file = uriToFile(imageUri) ?: return@launch
            try {
                // Chiffrement et upload de la couverture (Souveraineté maintenue)
                val storagePath = mediaManager.encryptAndUpload(uid, complementId, file)
                offlineEntryDao.updateEntryCover(storagePath, file.absolutePath, complementId)
                triggerSync(complementId)
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Erreur upload couverture", e)
            }
        }
    }

    fun updateRecipients(newRecipientDocIds: List<String>) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            // v9.2 : On stocke les VRAIS UIDs pour la sécurité Firestore
            val persistentIds = newRecipientDocIds.map { docId ->
                recipients.value.find { it.id == docId }?.linkedUid ?: docId
            }.distinct() // v9.4.19
            offlineEntryDao.updateEntryRecipients(persistentIds.joinToString(","), id)
            triggerSync(id)
        }
    }

    /**
     * Alterne la sélection d'un destinataire (v9.4.19)
     */
    fun toggleRecipient(docId: String) {
        val current = selectedRecipientIds.value
        val newList = if (current.contains(docId)) {
            current.filter { it != docId }
        } else {
            current + docId
        }
        updateRecipients(newList)
    }

    // --- GESTION DES PERSONNES (v9.4.26 : Unifiée & Stabilisée) ---

    private var searchJob: Job? = null

    fun searchPersons(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _suggestedPersons.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val persons = offlineEntryDao.getAllPersons().first().toSimplified()
                val recipientsList = offlineEntryDao.getAllRecipients().first().toSimplifiedRecipient()
                val witnesses = offlineEntryDao.getAllWitnesses().first().toSimplifiedWitness()
                val depositaries = offlineEntryDao.getAllDepositaries().first().toSimplifiedDepositary()

                val allSimplified = persons + recipientsList + witnesses + depositaries
                val filtered = allSimplified
                    .filter { it.name.contains(query, ignoreCase = true) }
                    .distinctBy { it.name.lowercase().trim() }
                
                _suggestedPersons.value = filtered
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Erreur recherche personnes", e)
            }
        }
    }

    fun selectPerson(person: SimplifiedPerson) {
        val currentIds = entry.value?.personIds?.split(",")
            ?.filter { it.isNotBlank() }?.map { it.trim() } ?: emptyList()
        
        if (!currentIds.contains(person.id)) {
            val newList = currentIds + person.id
            updatePersonsInDb(newList)
        }
        _suggestedPersons.value = emptyList()
    }

    fun selectMe() {
        val user = auth.currentUser ?: return
        val meId = "ME_${user.uid}"
        val currentIds = entry.value?.personIds?.split(",")
            ?.filter { it.isNotBlank() }?.map { it.trim() } ?: emptyList()

        if (!currentIds.contains(meId)) {
            val newList = currentIds + meId
            updatePersonsInDb(newList)
        }
    }

    fun removePerson(personId: String) {
        val currentIds = entry.value?.personIds?.split(",")
            ?.filter { it.isNotBlank() }?.map { it.trim() } ?: emptyList()
        
        val newList = currentIds.filter { it != personId }
        updatePersonsInDb(newList)
    }

    fun createAndSelectPerson(
        firstName: String,
        lastName: String?,
        relationship: String?,
        distinctionType: String?,
        distinctionValue: String?,
        imageUri: Uri?,
        characterType: String = "HUMAN"
    ) {
        viewModelScope.launch {
            var finalImagePath: String? = null
            if (imageUri != null) {
                try {
                    val cameoDir = File(context.filesDir, "cameos")
                    if (!cameoDir.exists()) cameoDir.mkdirs()
                    val fileName = "cameo_${UUID.randomUUID()}.jpg"
                    val destFile = File(cameoDir, fileName)
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        FileOutputStream(destFile).use { output -> input.copyTo(output) }
                    }
                    finalImagePath = destFile.absolutePath
                } catch (e: Exception) {
                    android.util.Log.e("CameoDebug", "Erreur sauvegarde portrait", e)
                }
            }

            try {
                val newPerson = PersonEntity(
                    firstName = firstName,
                    lastName = lastName,
                    relationship = relationship,
                    distinctionType = distinctionType,
                    distinctionValue = distinctionValue,
                    imagePath = finalImagePath,
                    characterType = characterType
                )
                offlineEntryDao.insertPerson(newPerson)
                
                // Point 1 : Conversion pour le sélecteur unifié
                val simplified = SimplifiedPerson(
                    id = newPerson.id,
                    name = newPerson.firstName + (newPerson.lastName?.let { l -> " $l" } ?: ""),
                    photoUrl = newPerson.imagePath,
                    sourceType = "arbre_livre",
                    relationship = newPerson.relationship
                )
                selectPerson(simplified)
            } catch (_: Exception) { }
        }
    }

    private fun updatePersonsInDb(ids: List<String>) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryPersons(ids.distinct().joinToString(","), id)
            triggerSync(id)
        }
    }

    /**
     * Met à jour la visibilité d'une entrée spécifique (v9.4.27)
     */
    fun updateEntryVisibility(entryId: String, visibility: String) {
        viewModelScope.launch {
            offlineEntryDao.updateEntryVisibility(visibility, entryId)
            triggerSync(entryId)
        }
    }

    /**
     * Met à jour les destinataires d'une entrée spécifique (v9.4.27)
     */
    fun updateEntryRecipients(entryId: String, recipientDocIds: List<String>) {
        viewModelScope.launch {
            val persistentIds = recipientDocIds.map { docId ->
                recipients.value.find { it.id == docId }?.linkedUid ?: docId
            }.distinct()
            offlineEntryDao.updateEntryRecipients(persistentIds.joinToString(","), entryId)
            triggerSync(entryId)
        }
    }

    fun updateVisibility(visibility: String) {
        val id = _entryId.value ?: return
        updateEntryVisibility(id, visibility)
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
                if (isParent) {
                    // SUPPRESSION EN CASCADE (v9.4.27)
                    val parent = offlineEntryDao.getEntryById(id).first()
                    val children = offlineEntryDao.getComplements(id).first()
                    
                    // 1. Suppression Storage (Parent + Enfants + Couvertures)
                    parent?.let {
                        mediaManager.deleteFile(it.mediaUrl)
                        mediaManager.deleteFile(it.coverUrl)
                    }
                    children.forEach { child ->
                        mediaManager.deleteFile(child.mediaUrl)
                        mediaManager.deleteFile(child.coverUrl)
                    }

                    // 2. Suppression Firestore du parent et de chaque enfant
                    val batch = db.batch()
                    val userRef = db.collection("users").document(uid)
                    
                    batch.delete(userRef.collection("entries").document(id))
                    children.forEach { child ->
                        batch.delete(userRef.collection("entries").document(child.id))
                    }
                    batch.commit().await()
                    
                    // 3. Suppression Room locale (Parent + Enfants via le DAO)
                    offlineEntryDao.deleteEntry(id) // Le parent
                    children.forEach { child ->
                        offlineEntryDao.deleteEntry(child.id) // Les enfants
                    }
                    
                    _deleteSuccess.value = true
                } else {
                    // Suppression simple d'un complément
                    val complement = offlineEntryDao.getEntryById(id).first()
                    complement?.let {
                        mediaManager.deleteFile(it.mediaUrl)
                        mediaManager.deleteFile(it.coverUrl)
                    }

                    db.collection("users").document(uid)
                        .collection("entries").document(id)
                        .delete()
                        .await()
                    offlineEntryDao.deleteEntry(id)
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

    /**
     * Ajoute un complément média directement (v9.4.26)
     * v9.4.27 : Extraction automatique de miniature pour VIDEO.
     */
    fun addMediaComplement(parentId: String, file: File, type: String, transcription: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Copie locale de la vidéo
                val mediaDir = File(context.filesDir, "media")
                if (!mediaDir.exists()) mediaDir.mkdirs()
                val destFile = File(mediaDir, "PHX_COMP_${UUID.randomUUID()}_${file.name}")
                file.inputStream().use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
                
                var coverUrl: String? = null
                var localCoverPath: String? = null

                // 2. Extraction de la miniature si c'est une VIDEO (v9.4.27)
                if (type == "VIDEO") {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(destFile.absolutePath)
                        val bitmap = retriever.getFrameAtTime(0)
                        retriever.release()

                        if (bitmap != null) {
                            val thumbFile = File(mediaDir, "THUMB_${destFile.name}.jpg")
                            FileOutputStream(thumbFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                            }
                            // Chiffrement et upload de la miniature
                            coverUrl = mediaManager.encryptAndUpload(uid, UUID.randomUUID().toString(), thumbFile)
                            localCoverPath = thumbFile.absolutePath
                            android.util.Log.d("MemoryDetailVM", "Miniature vidéo extraite et uploadée : $coverUrl")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MemoryDetailVM", "Échec extraction miniature vidéo", e)
                    }
                }

                // 3. Récupération du parent pour héritage
                val parent = offlineEntryDao.getEntryById(parentId).first() ?: return@launch
                
                val finalTranscription = if (transcription.isNullOrBlank()) "Média complémentaire" else transcription

                val entry = OfflineEntry(
                    id = UUID.randomUUID().toString(),
                    creatorUid = parent.creatorUid,
                    encryptedPayload = encryptionManager.encryptText(finalTranscription),
                    entryType = type,
                    ageAtCreation = parent.ageAtCreation,
                    emotionalCategory = parent.emotionalCategory,
                    visibility = parent.visibility,
                    recipientIds = parent.recipientIds,
                    parentEntryId = parentId,
                    localMediaPath = destFile.absolutePath,
                    coverUrl = coverUrl,
                    localCoverPath = localCoverPath,
                    aiSummary = finalTranscription,
                    syncStatus = "pending"
                )
                offlineEntryDao.insertEntry(entry)
                triggerSync(entry.id)
            } catch (e: Exception) {
                android.util.Log.e("MemoryDetailVM", "Erreur ajout média", e)
                _error.value = "Erreur lors de l'ajout du média"
            }
        }
    }

    fun uriToFile(uri: android.net.Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            
            // v9.4.27 : Détection robuste de l'extension (inclut support caméra)
            val extension = when {
                mimeType?.contains("video") == true -> "mp4"
                uri.toString().contains(".mp4") -> "mp4"
                else -> "jpg"
            }
            
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_media_${UUID.randomUUID()}.$extension")
            inputStream?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
