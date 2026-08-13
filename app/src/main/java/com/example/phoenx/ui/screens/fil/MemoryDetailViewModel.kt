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
import com.example.phoenx.data.sync.toOfflineEntry
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
    private val mediaManager: com.example.phoenx.data.media.MediaManager,
    private val preferenceManager: com.example.phoenx.data.preferences.PreferenceManager,
    private val livingLinkService: com.example.phoenx.data.living.LivingLinkService, // v9.4.27
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
    private val _targetCreatorId = MutableStateFlow<String?>(null) // v9.4.27
    private val _firestoreEntry = MutableStateFlow<OfflineEntry?>(null) // v9.4.27

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    enum class ProtocolStatus { VERIFYING, ACTIVATED, LOCKED }
    private val _protocolStatus = MutableStateFlow(ProtocolStatus.VERIFYING)
    val protocolStatus: StateFlow<ProtocolStatus> = _protocolStatus.asStateFlow()

    val hasSeenIncludeInBookNudge: StateFlow<Boolean> = preferenceManager.hasSeenIncludeInBookNudge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun markIncludeInBookNudgeSeen() {
        viewModelScope.launch {
            preferenceManager.markIncludeInBookNudgeSeen()
        }
    }

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _suggestedPersons = MutableStateFlow<List<SimplifiedPerson>>(emptyList())
    val suggestedPersons: StateFlow<List<SimplifiedPerson>> = _suggestedPersons.asStateFlow()

    val recipients: StateFlow<List<RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPacts: StateFlow<List<PactEntity>> = offlineEntryDao.getAllPacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Source de vérité hybride (v9.4.27 : Room ou Firestore)
     */
    val entry: StateFlow<OfflineEntry?> = combine(_entryId, _targetCreatorId, _firestoreEntry) { id, targetId, fsEntry ->
        if (id == null) return@combine null
        
        val isHeirMode = targetId != null && targetId != auth.currentUser?.uid
        if (isHeirMode) {
            // Mode Héritier : Source Firestore
            fsEntry
        } else {
            // Mode Créateur : Source Room locale
            offlineEntryDao.getEntryById(id).firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
            val displayName = if (user.displayName.isNullOrBlank()) "Moi" else user.displayName!!
            listOf(SimplifiedPerson(
                id = "ME_${user.uid}",
                name = displayName,
                photoUrl = user.photoUrl?.toString(),
                sourceType = "auteur",
                relationship = "Auteur",
                isMe = true
            ))
        } else emptyList()

        (allSimplified + me).filter { ids.contains(it.id) }.distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _firestoreComplements = MutableStateFlow<List<OfflineEntry>>(emptyList()) // v9.4.27

    val complements: StateFlow<List<OfflineEntry>> = combine(_entryId, _targetCreatorId, _firestoreComplements) { id, targetId, fsComps ->
        if (id == null) return@combine emptyList()
        val isHeirMode = targetId != null && targetId != auth.currentUser?.uid
        if (isHeirMode) {
            fsComps
        } else {
            offlineEntryDao.getComplements(id).first()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Retourne la liste des compléments texte DÉCHIFFRÉS (v8.4)
     */
    val decryptedTextComplements: StateFlow<List<Pair<String, String>>> = combine(complements, _heirKey, _protocolStatus) { list, key, status ->
        when (status) {
            ProtocolStatus.VERIFYING -> emptyList()
            ProtocolStatus.LOCKED -> list.filter { it.entryType == "TEXT" || it.entryType == "THOUGHT" }
                .map { it.id to "Souvenir scellé" }
            ProtocolStatus.ACTIVATED -> list.filter { (it.entryType == "TEXT") || (it.entryType == "THOUGHT") }
                .map { it.id to encryptionManager.decryptText(it.encryptedPayload, key) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decryptedContent: StateFlow<String> = combine(entry, _heirKey, _protocolStatus) { ent, key, status ->
        android.util.Log.d("PHOENX_DETAIL_TRACE", "--- RECALCUL CONTENT --- Status: $status, Key: ${key != null}")
        when (status) {
            ProtocolStatus.VERIFYING -> "Vérification de l'accès..."
            ProtocolStatus.LOCKED -> "Souvenir scellé"
            ProtocolStatus.ACTIVATED -> ent?.let { 
                try {
                    val res = encryptionManager.decryptText(it.encryptedPayload, key)
                    android.util.Log.d("PHOENX_DETAIL_TRACE", "SUCCÈS DECHIFFREMENT id=${it.id}")
                    res
                } catch(e: Exception) {
                    android.util.Log.e("PHOENX_DETAIL_TRACE", "ÉCHEC DECHIFFREMENT id=${it.id}: ${e.message}", e)
                    "Contenu chiffré"
                }
            } ?: ""
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /**
     * Modèle structuré pour les items du Portrait (v8.5.7)
     */
    data class PortraitItem(val id: String?, val question: String, val answer: String)

    /**
     * Fusionne le contenu legacy et les nouveaux compléments atomiques (v8.5.7)
     */
    val structuredPortrait: StateFlow<List<PortraitItem>> = combine(decryptedContent, complements, _heirKey, _protocolStatus) { content, compList, key, status ->
        val list = mutableListOf<PortraitItem>()
        
        if (status == ProtocolStatus.VERIFYING) return@combine list
        if (status == ProtocolStatus.LOCKED && key != null) {
            // Uniquement les compléments atomiques pour les titres, mais contenu scellé
            compList.filter { it.parentEntryId == _entryId.value && it.entryType == "TEXT" }.forEach { comp ->
                list.add(PortraitItem(comp.id, comp.aiSummary, "Souvenir scellé"))
            }
            return@combine list
        }

        // 1. Parsing Legacy (v8.5.9 - Titres intelligents)
        if (content.isNotBlank()) {
            if (content.startsWith("[")) {
                try {
                    val arr = JSONArray(content)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val q = obj.getString("q")
                        val a = obj.getString("a")
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
        _targetCreatorId.value = creatorId
        
        if (creatorId != null && creatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                _protocolStatus.value = ProtocolStatus.VERIFYING
                try {
                    // 1. Check protocol status
                    val result = functions.getHttpsCallable("getCreatorProtocolStatus")
                        .call(mapOf("creatorId" to creatorId)).await()
                    
                    val data = result.data as? Map<*, *>
                    val isActivated = data?.get("isActivated") as? Boolean ?: false
                    _protocolStatus.value = if (isActivated) ProtocolStatus.ACTIVATED else ProtocolStatus.LOCKED
                    android.util.Log.d("PHOENX_DETAIL_TRACE", "Protocole check: isActivated=$isActivated -> status=${_protocolStatus.value}")

                    if (isActivated) {
                        val keyDoc = db.collection("users").document(creatorId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                        }
                    }
                    
                    // 2. CHARGEMENT FIRESTORE (v9.4.27 : Lecture Hybride Héritier)
                    val entryDoc = db.collection("users").document(creatorId)
                        .collection("entries").document(id).get().await()
                    
                    if (entryDoc.exists()) {
                        _firestoreEntry.value = entryDoc.toOfflineEntry(encryptionManager, _heirKey.value)
                        
                        // Charger aussi les compléments
                        val compSnap = db.collection("users").document(creatorId)
                            .collection("entries")
                            .whereEqualTo("parentEntryId", id)
                            .get().await()
                        
                        _firestoreComplements.value = compSnap.documents.mapNotNull { it.toOfflineEntry(encryptionManager, _heirKey.value) }
                    }

                } catch (e: Exception) {
                    android.util.Log.e("MemoryDetailVM", "Erreur chargement distant", e)
                    _protocolStatus.value = ProtocolStatus.LOCKED
                }
            }
        } else {
            _protocolStatus.value = ProtocolStatus.ACTIVATED
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
                val encrypted = encryptionManager.encryptText(newText)
                offlineEntryDao.updateEntryContent(encrypted, id)
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

    fun updateComplementTitle(complementId: String, newTitle: String) {
        viewModelScope.launch {
            offlineEntryDao.updateEntryMediaTitle(newTitle, complementId)
            triggerSync(complementId)
        }
    }

    fun updateComplementComment(complementId: String, newComment: String?) {
        viewModelScope.launch {
            offlineEntryDao.updateEntryComment(newComment, complementId)
            triggerSync(complementId)
        }
    }

    fun updateComplementCover(complementId: String, imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val file = uriToFile(imageUri) ?: return@launch
            try {
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
            val persistentIds = newRecipientDocIds.map { docId ->
                recipients.value.find { it.id == docId }?.linkedUid ?: docId
            }.distinct()
            offlineEntryDao.updateEntryRecipients(persistentIds.joinToString(","), id)
            triggerSync(id)
        }
    }

    fun toggleRecipient(docId: String) {
        val current = selectedRecipientIds.value
        val newList = if (current.contains(docId)) {
            current.filter { it != docId }
        } else {
            current + docId
        }
        updateRecipients(newList)
    }

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

    fun updateEnigma(
        question: String?,
        answer: String?,
        hint: String?,
        autoUnlockDays: Int?,
        isUltimate: Boolean
    ) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            val currentEntry = offlineEntryDao.getEntryById(id).first() ?: return@launch
            val newHash = if (!answer.isNullOrBlank()) {
                com.example.phoenx.domain.util.EnigmaUtils.hashAnswer(answer)
            } else {
                currentEntry.enigmaAnswer
            }

            offlineEntryDao.updateEntryEnigma(
                question = question,
                answerHash = newHash,
                hint = hint,
                unlockDays = autoUnlockDays,
                isUltimate = isUltimate,
                entryId = id
            )
            triggerSync(id)
        }
    }
    fun updateEntryVisibility(entryId: String, visibility: String) {
        viewModelScope.launch {
            offlineEntryDao.updateEntryVisibility(visibility, entryId)
            triggerSync(entryId)
        }
    }

    fun updateEntryRecipients(entryId: String, recipientDocIds: List<String>) {
        viewModelScope.launch {
            val persistentIds = recipientDocIds.map { docId ->
                recipients.value.find { it.id == docId }?.linkedUid ?: docId
            }.distinct()
            offlineEntryDao.updateEntryRecipients(persistentIds.joinToString(","), entryId)
            triggerSync(entryId)
        }
    }

    fun sendLivingLink(recipientUid: String, scheduledAt: Long? = null) {
        val entryId = _entryId.value ?: return
        viewModelScope.launch {
            try {
                livingLinkService.sendLivingLink(entryId, recipientUid, scheduledAt)
                _error.value = "Souvenir transmis avec succès !"
            } catch (e: Exception) {
                android.util.Log.e("LivingLink", "Erreur transmission", e)
                _error.value = "Échec de la transmission : ${e.message}"
            }
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

    fun updateIncludeInBook(include: Boolean) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryIncludeInBook(include, id)
            triggerSync(id)
        }
    }

    fun updatePactId(pactId: String?) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryPactId(pactId, id)
            triggerSync(id)
        }
    }

    fun updateCompartments(selectedIds: List<String>) {
        val id = _entryId.value ?: return
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

    fun updateTonalNuance(nuance: String) {
        val id = _entryId.value ?: return
        viewModelScope.launch {
            offlineEntryDao.updateEntryTonalNuance(nuance.take(150), id)
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
                    val parent = offlineEntryDao.getEntryById(id).first()
                    val children = offlineEntryDao.getComplements(id).first()
                    
                    parent?.let {
                        mediaManager.deleteFile(it.mediaUrl)
                        mediaManager.deleteFile(it.coverUrl)
                    }
                    children.forEach { child ->
                        mediaManager.deleteFile(child.mediaUrl)
                        mediaManager.deleteFile(child.coverUrl)
                    }

                    val batch = db.batch()
                    val userRef = db.collection("users").document(uid)
                    
                    batch.delete(userRef.collection("entries").document(id))
                    children.forEach { child ->
                        batch.delete(userRef.collection("entries").document(child.id))
                    }
                    batch.commit().await()
                    
                    offlineEntryDao.deleteEntry(id)
                    children.forEach { child ->
                        offlineEntryDao.deleteEntry(child.id)
                    }
                    
                    _deleteSuccess.value = true
                } else {
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
        offlineEntryDao.updateSyncStatus(entryId, "pending")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    fun addMediaComplement(parentId: String, file: File, type: String, transcription: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val mediaDir = File(context.filesDir, "media")
                if (!mediaDir.exists()) mediaDir.mkdirs()
                val destFile = File(mediaDir, "PHX_COMP_${UUID.randomUUID()}_${file.name}")
                file.inputStream().use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
                
                var coverUrl: String? = null
                var localCoverPath: String? = null

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
                            coverUrl = mediaManager.encryptAndUpload(uid, UUID.randomUUID().toString(), thumbFile)
                            localCoverPath = thumbFile.absolutePath
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MemoryDetailVM", "Échec extraction miniature vidéo", e)
                    }
                }

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
