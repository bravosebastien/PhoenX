package com.example.phoenx.ui.screens.recipient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Blob
import kotlinx.coroutines.channels.awaitClose
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipientMediaViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: com.example.phoenx.data.local.StandaloneMediaDao, // v9.3.2
    private val encryptionManager: EncryptionManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    val mediaManager: com.example.phoenx.data.media.MediaManager
) : ViewModel() {

    private val _libraryEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val libraryEntries: StateFlow<List<PhoenXEntry>> = _libraryEntries

    private val _discothequeEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val discothequeEntries: StateFlow<List<PhoenXEntry>> = _discothequeEntries

    private val _archiveEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val archiveEntries: StateFlow<List<PhoenXEntry>> = _archiveEntries

    private val _videoEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val videoEntries: StateFlow<List<PhoenXEntry>> = _videoEntries

    private val _heritageEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val heritageEntries: StateFlow<List<PhoenXEntry>> = _heritageEntries

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    private val _isProtocolActivated = MutableStateFlow(true)
    val isProtocolActivated: StateFlow<Boolean> = _isProtocolActivated.asStateFlow()

    private val _canAskQuestions = MutableStateFlow(false)
    val canAskQuestions: StateFlow<Boolean> = _canAskQuestions.asStateFlow()

    private val _recipientId = MutableStateFlow<String?>(null)
    val recipientId: StateFlow<String?> = _recipientId.asStateFlow()

    private val _bookSealedMessage = MutableStateFlow<String?>(null)
    val bookSealedMessage: StateFlow<String?> = _bookSealedMessage.asStateFlow()

    private val _bookTitle = MutableStateFlow<String?>(null)
    val bookTitle: StateFlow<String?> = _bookTitle.asStateFlow()

    private val _creatorName = MutableStateFlow("Votre proche")
    val creatorName: StateFlow<String> = _creatorName.asStateFlow()

    private val _targetCreatorId = MutableStateFlow<String?>(null)

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    init {
        loadAllMedia()
    }

    fun setTargetCreator(creatorId: String?) {
        _targetCreatorId.value = creatorId
        if (creatorId != null && creatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                try {
                    // Fetch Creator Name (v8.6.2)
                    val creatorDoc = db.collection("users").document(creatorId).get().await()
                    _creatorName.value = creatorDoc.getString("displayName") ?: "Votre proche"

                    // Check protocol status via Cloud Function (v8.5.9)
                    val result = functions.getHttpsCallable("getCreatorProtocolStatus")
                        .call(mapOf("creatorId" to creatorId)).await()
                    
                    val data = result.data as? Map<*, *>
                    _isProtocolActivated.value = data?.get("isActivated") as? Boolean ?: false
                    _bookSealedMessage.value = data?.get("sealedMessage") as? String

                    // Fetch Book Title (v9.2)
                    val bookDoc = db.collection("users").document(creatorId)
                        .collection("book").document("current_draft").get().await()
                    _bookTitle.value = bookDoc.getString("bookTitle")

                    // v9.2.6 : Fetch My Permissions (canAskQuestions)
                    val recipientsSnapshot = db.collection("users").document(creatorId)
                        .collection("recipients")
                        .whereEqualTo("linkedUid", currentUid)
                        .get().await()
                    
                    if (!recipientsSnapshot.isEmpty) {
                        val recipientDoc = recipientsSnapshot.documents.first()
                        _canAskQuestions.value = recipientDoc.getBoolean("canAskQuestions") ?: false
                        _recipientId.value = recipientDoc.id
                    }

                    if (_isProtocolActivated.value) {
                        val keyDoc = db.collection("users").document(creatorId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            _heirKey.value = android.util.Base64.decode(keyBase64 as String, android.util.Base64.NO_WRAP)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecipientMediaVM", "Erreur chargement clé héritage: ${e.message}")
                    _isProtocolActivated.value = false
                }
            }
        } else {
            _isProtocolActivated.value = true
            _heirKey.value = null
            // Mode Créateur : Charger son propre titre de livre (v9.2)
            viewModelScope.launch {
                try {
                    val bookDoc = db.collection("users").document(currentUid)
                        .collection("book").document("current_draft").get().await()
                    _bookTitle.value = bookDoc.getString("bookTitle")
                } catch (e: Exception) {
                    _bookTitle.value = null
                }
            }
        }
    }

    fun addStandaloneMedia(
        title: String, 
        content: String, 
        type: String, 
        recipientIds: List<String>,
        userComment: String? = null,
        existingId: String? = null,
        visibility: String = "RESTRICTED"
    ) {
        viewModelScope.launch {
            val mediaId = existingId ?: java.util.UUID.randomUUID().toString()
            val needsEncryption = type == "TEXT_EXCERPT" || type == "PHOTO"
            
            val finalContent = if (needsEncryption) {
                val encrypted = encryptionManager.encryptText(content)
                android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
            } else {
                content
            }

            val entity = com.example.phoenx.data.local.StandaloneMediaEntity(
                id = mediaId,
                creatorUid = currentUid,
                type = type,
                title = title,
                userComment = userComment,
                content = finalContent,
                recipientIds = recipientIds.distinct().joinToString(","),
                visibility = visibility,
                createdAt = System.currentTimeMillis(),
                syncStatus = "pending"
            )

            standaloneMediaDao.insertMedia(entity)
        }
    }

    /**
     * Supprime un média (Standalone ou rattaché à un souvenir) - v9.4.27
     */
    fun deleteMediaEntry(entry: PhoenXEntry) {
        viewModelScope.launch {
            try {
                if (entry.parentEntryId != null) {
                    // C'est un complément : On utilise la logique de MemoryDetail
                    // Mais on l'implémente ici pour éviter les dépendances croisées de VM
                    val uid = auth.currentUser?.uid ?: return@launch
                    
                    // 1. Suppression Storage (v9.4.27)
                    mediaManager.deleteFile(entry.mediaUrl)
                    
                    // 2. Suppression Firestore
                    db.collection("users").document(uid)
                        .collection("entries").document(entry.id)
                        .delete().await()
                    
                    // 3. Suppression Room locale
                    offlineEntryDao.deleteEntry(entry.id)
                    android.util.Log.d("RecipientMediaVM", "Complément supprimé: ${entry.id}")
                } else {
                    // C'est un standalone : Logique existante
                    deleteStandaloneMedia(entry)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientMediaVM", "Erreur lors de la suppression du média ${entry.id}", e)
            }
        }
    }

    /**
     * Met à jour un média (v9.4.27)
     */
    fun updateMediaEntry(id: String, title: String, comment: String?, url: String, recipientIds: List<String>, visibility: String, isComplement: Boolean) {
        viewModelScope.launch {
            try {
                if (isComplement) {
                    // Note Vocale rattachée
                    offlineEntryDao.updateEntryMediaTitle(title, id)
                    offlineEntryDao.updateEntryComment(comment, id)
                    offlineEntryDao.updateEntryVisibility(visibility, id)
                    offlineEntryDao.updateEntryRecipients(recipientIds.joinToString(","), id)
                } else {
                    // Média isolé
                    standaloneMediaDao.updateMedia(id, title, comment, url, recipientIds.joinToString(","), visibility)
                }
                if (isComplement) offlineEntryDao.updateSyncStatus(id, "pending")
                else standaloneMediaDao.updateSyncStatus(id, "pending")
            } catch (e: Exception) {
                android.util.Log.e("RecipientMediaVM", "Erreur lors de la mise à jour du média $id", e)
            }
        }
    }

    /**
     * Supprime un média Standalone (v9.3.3)
     */
    private fun deleteStandaloneMedia(media: PhoenXEntry) {
        viewModelScope.launch {
            try {
                // 1. Suppression Firestore
                db.collection("users").document(currentUid)
                    .collection("standaloneMedia").document(media.id).delete().await()
                
                // 2. Suppression Room locale
                // Note: On devrait idéalement avoir un DAO delete by ID
                standaloneMediaDao.getAllStandaloneMedia().first().find { it.id == media.id }?.let {
                    standaloneMediaDao.deleteMedia(it)
                }

                // 3. Suppression Storage si c'est une photo
                if (media.type == EntryType.PHOTO) {
                    try {
                        // On essaie de supprimer le fichier enc dans /standalone_photos/
                        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                            .child("users").child(currentUid).child("standalone_photos")
                            .child("${media.id}.jpg.enc")
                        storageRef.delete().await()
                    } catch (e: Exception) {
                        android.util.Log.w("RecipientMediaVM", "Fichier storage introuvable pour suppression")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipientMediaVM", "Erreur suppression standalone: ${e.message}")
            }
        }
    }

    /**
     * Gère l'upload chiffré d'une photo et son enregistrement Standalone (v9.3.2)
     */
    fun uploadAndAddStandalonePhoto(title: String, userComment: String?, localFile: java.io.File, recipientIds: List<String>) {
        viewModelScope.launch {
            try {
                val mediaId = java.util.UUID.randomUUID().toString()
                // 1. Upload chiffré vers Storage
                val downloadUrl = mediaManager.encryptAndUploadStandalone(currentUid, mediaId, localFile)
                
                // 2. Enregistrement de l'entité avec l'URL (qui sera elle-même chiffrée dans Firestore)
                addStandaloneMedia(title, downloadUrl, "PHOTO", recipientIds, userComment, mediaId)
            } catch (e: Exception) {
                android.util.Log.e("RecipientMediaVM", "Erreur upload photo standalone: ${e.message}")
            }
        }
    }

    /**
     * Met à jour un média standalone existant (v9.4.27)
     */
    fun updateStandaloneMedia(id: String, title: String, userComment: String?, url: String, recipientIds: List<String>, visibility: String) {
        viewModelScope.launch {
            try {
                standaloneMediaDao.updateMedia(id, title, userComment, url, recipientIds.joinToString(","), visibility)
            } catch (e: Exception) {
                android.util.Log.e("RecipientMediaVM", "Erreur update standalone", e)
            }
        }
    }

    /**
     * Chiffre et uploade une photo de couverture pour un média (v9.4.27)
     * Gère Standalone et Compléments.
     */
    fun updateMediaCover(id: String, imageUri: android.net.Uri, isComplement: Boolean) {
        val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
        viewModelScope.launch {
            val file = try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val tempFile = java.io.File(context.cacheDir, "temp_cover_${java.util.UUID.randomUUID()}.jpg")
                inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                tempFile
            } catch(_: Exception) { null } ?: return@launch
            
            try {
                val storagePath = mediaManager.encryptAndUpload(currentUid, id, file)
                if (isComplement) {
                    offlineEntryDao.updateEntryCover(storagePath, file.absolutePath, id)
                    offlineEntryDao.updateSyncStatus(id, "pending")
                } else {
                    standaloneMediaDao.updateMediaCover(id, storagePath, file.absolutePath)
                    standaloneMediaDao.updateSyncStatus(id, "pending")
                }
                android.util.Log.d("RecipientMediaVM", "Couverture mise à jour pour $id")
            } catch (e: Exception) {
                android.util.Log.e("RecipientMediaVM", "Erreur upload couverture", e)
            }
        }
    }

    private fun loadAllMedia() {
        val currentUid = auth.currentUser?.uid ?: ""
        viewModelScope.launch {
            val offlineEntriesFlow = _targetCreatorId.flatMapLatest { targetId ->
                if (targetId == null || targetId == currentUid) {
                    offlineEntryDao.getAllEntries()
                } else {
                    val publicFlow = callbackFlow {
                        val listener = db.collection("users").document(targetId)
                            .collection("entries")
                            .whereEqualTo("visibility", "EVERYONE")
                            .addSnapshotListener { snapshot, _ ->
                                trySend(snapshot?.documents?.mapNotNull { it.toOfflineEntry() } ?: emptyList())
                            }
                        awaitClose { listener.remove() }
                    }

                    val privateFlow = callbackFlow {
                        val listener = db.collection("users").document(targetId)
                            .collection("entries")
                            .whereArrayContains("recipientIds", currentUid)
                            .addSnapshotListener { snapshot, _ ->
                                trySend(snapshot?.documents?.mapNotNull { it.toOfflineEntry() } ?: emptyList())
                            }
                        awaitClose { listener.remove() }
                    }

                    combine(publicFlow, privateFlow) { pub, priv ->
                        (pub + priv).distinctBy { it.id }
                    }
                }
            }

            val standaloneMediaFlow = _targetCreatorId.flatMapLatest { targetId ->
                if (targetId == null || targetId == currentUid) {
                    standaloneMediaDao.getAllStandaloneMedia()
                } else {
                    // Lecture Firestore directe pour les héritiers (v9.3.2)
                    callbackFlow {
                        val listener = db.collection("users").document(targetId)
                            .collection("standaloneMedia")
                            .addSnapshotListener { snapshot, _ ->
                                val list = snapshot?.documents?.mapNotNull { doc ->
                                    val recIds = (doc.get("recipientIds") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                                    // Filtrage visibilité v9.3.2
                                    if (recIds.isEmpty() || recIds.contains(currentUid)) {
                                        val type = doc.getString("type") ?: ""
                                        val needsEncryption = type == "TEXT_EXCERPT" || type == "PHOTO"
                                        
                                        val contentStr = if (needsEncryption) {
                                            val blob = doc.get("content") as? Blob
                                            blob?.toBytes()?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) } ?: ""
                                        } else {
                                            doc.getString("content") ?: ""
                                        }

                                        com.example.phoenx.data.local.StandaloneMediaEntity(
                                            id = doc.id,
                                            creatorUid = targetId,
                                            type = type,
                                            title = doc.getString("title") ?: "",
                                            userComment = doc.getString("userComment"), // v9.4.27
                                            content = contentStr,
                                            recipientIds = recIds.joinToString(","),
                                            visibility = doc.getString("visibility") ?: "RESTRICTED",
                                            createdAt = doc.getLong("createdAt") ?: 0L,
                                            syncStatus = "synced",
                                            coverUrl = doc.getString("coverUrl"),
                                            mediaProvider = doc.getString("mediaProvider")
                                        )
                                    } else null
                                } ?: emptyList()
                                trySend(list)
                            }
                        awaitClose { listener.remove() }
                    }
                }
            }

            combine(offlineEntriesFlow, standaloneMediaFlow, _isProtocolActivated) { allOfflineEntries, allStandalone, activated ->
                val targetId = _targetCreatorId.value
                val isHeirMode = targetId != null && targetId != currentUid

                // 1. Filtrage par ACCÈS STRICT (Entries classiques)
                val accessibleEntries = if (!isHeirMode) {
                    allOfflineEntries 
                } else {
                    allOfflineEntries.filter { entry ->
                        entry.visibility == "EVERYONE" || entry.recipientIds.split(",").filter { it.isNotBlank() }.map { it.trim() }.contains(currentUid)
                    }
                }

                // 2. Conversion en domaine (Déchiffrement Tink pour les entries)
                val decodedEntries = accessibleEntries.map { 
                    if (isHeirMode && !activated) it.toSealedDomain()
                    else it.toDomain(encryptionManager) 
                }.toMutableList()

                // 3. Conversion et Injection des Standalone Media (v9.3.2)
                allStandalone.forEach { standalone ->
                    val domainEntry = standalone.toStandaloneDomain(isHeirMode, activated)
                    decodedEntries.add(domainEntry)
                }

                Triple(decodedEntries.toList(), activated, isHeirMode)
            }
            .flowOn(Dispatchers.Default)
            .collectLatest { (allDecoded, activated, _) ->
                // 4. Indexation par type (v9.4.27 : Chaque média, parent ou complément, est listé individuellement)
                _libraryEntries.value = allDecoded.filter { it.parentEntryId == null && (it.type == EntryType.THOUGHT || it.type == EntryType.LEGACY || it.isYoungSelfLetter) }

                _videoEntries.value = allDecoded.filter { it.type == EntryType.VIDEO }

                _discothequeEntries.value = allDecoded.filter { it.type == EntryType.AUDIO }

                _archiveEntries.value = allDecoded.filter { it.type == EntryType.PHOTO }

                // 5. Unified Heritage List (v8.5.3) - On ne garde que les parents pour le flux chronologique principal
                _heritageEntries.value = allDecoded.filter { it.parentEntryId == null }.sortedByDescending { it.timestamp }
            }
        }
    }

    /**
     * Helper pour convertir un document Firestore en OfflineEntry (v8.5.5)
     */
    private fun DocumentSnapshot.toOfflineEntry(): OfflineEntry? {
        if (!exists()) return null
        val ageMap = get("ageAtCreation") as? Map<*, *>
        val ageJson = ageMap?.let { JSONObject(it).toString() } ?: "{}"

        val recIds = (get("recipientIds") as? List<*>)?.joinToString(",") ?: ""
        val compIds = (get("compartmentIds") as? List<*>)?.joinToString(",") ?: ""

        // DÉTECTION & DÉCHIFFREMENT AVEC CLÉ HÉRITIER (v9.4.12)
        val heirKey = _heirKey.value

        val summaryObj = get("aiSummary")
        val finalSummary = when (summaryObj) {
            is Blob -> encryptionManager.decryptText(summaryObj.toBytes(), heirKey)
            is String -> summaryObj
            else -> ""
        }

        val tagsObj = get("aiTags")
        val finalTags = when (tagsObj) {
            is Blob -> encryptionManager.decryptText(tagsObj.toBytes(), heirKey)
            is List<*> -> tagsObj.joinToString(",")
            is String -> tagsObj
            else -> ""
        }

        return OfflineEntry(
            id = id,
            creatorUid = getString("uid") ?: "",
            encryptedPayload = (get("encryptedContent") as? Blob)?.toBytes() ?: ByteArray(0),
            entryType = getString("type") ?: "TEXT",
            ageAtCreation = ageJson,
            emotionalCategory = getString("emotionalCategory") ?: "",
            visibility = getString("visibility") ?: "private",
            recipientIds = recIds,
            compartmentIds = compIds,
            isYoungSelfLetter = getBoolean("isYoungSelfLetter") ?: false,
            targetAge = getLong("targetAge")?.toInt(),
            createdAt = getLong("createdAt") ?: 0L,
            aiSummary = finalSummary,
            aiTags = finalTags,
            mediaUrl = getString("mediaUrl"),
            localMediaPath = null, // Pas de chemin local pour les entrées héritées
            memoryDate = getLong("memoryDate"),
            memoryDateStart = getLong("memoryDateStart"),
            memoryDateEnd = getLong("memoryDateEnd"),
            parentEntryId = getString("parentEntryId")
        )
    }

    private fun OfflineEntry.toSealedDomain(): PhoenXEntry {
        val ageJson = JSONObject(ageAtCreation)
        val age = AgeSnapshot(
            years = ageJson.optInt("years", 0),
            months = ageJson.optInt("months", 0),
            days = ageJson.optInt("days", 0)
        )
        
        val typeLabel = when(entryType) {
            "PHOTO" -> "Photo scellée"
            "VIDEO" -> "Vidéo scellée"
            "AUDIO" -> "Souvenir vocal scellé"
            "PORTRAIT" -> "Portrait scellé"
            "QUESTION_ANSWER" -> "Réponse scellée"
            else -> "Pensée scellée"
        }

        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            ageAtCreation = age,
            encryptedContent = "Ce contenu sera déchiffré lors de l'activation du protocole.".toByteArray(),
            type = when(entryType) {
                "PORTRAIT" -> EntryType.PORTRAIT
                "QUESTION_ANSWER" -> EntryType.QUESTION_ANSWER
                else -> try { EntryType.valueOf(entryType) } catch(_: Exception) { EntryType.THOUGHT }
            },
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary.ifBlank { typeLabel },
            hasEnigma = enigmaQuestion != null
        )
    }

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager): PhoenXEntry {
        val decryptedText = try { 
            encryptionManager.decryptText(encryptedPayload)
        } catch(_: Exception) { "Contenu chiffré" }
        
        val ageJson = JSONObject(ageAtCreation)
        val age = AgeSnapshot(
            years = ageJson.getInt("years"),
            months = ageJson.getInt("months"),
            days = ageJson.getInt("days")
        )

        val domainType = when(entryType) {
            "TEXT" -> EntryType.THOUGHT
            "AUDIO", "EMOTION" -> EntryType.AUDIO // v9.4.27 : Unification du type AUDIO
            "PHOTO" -> EntryType.PHOTO
            "VIDEO" -> EntryType.VIDEO
            else -> try { EntryType.valueOf(entryType) } catch(_: Exception) { EntryType.THOUGHT }
        }

        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            ageAtCreation = age,
            encryptedContent = decryptedText.toByteArray(),
            type = domainType,
            isYoungSelfLetter = isYoungSelfLetter,
            targetAge = targetAge,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary,
            userComment = userComment,
            parentEntryId = parentEntryId,
            mediaUrl = mediaUrl,
            localMediaPath = localMediaPath,
            coverUrl = coverUrl,
            localCoverPath = localCoverPath,
            mediaProvider = mediaProvider,
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() },
            visibility = visibility,
            silentAttribution = silentAttribution
        )
    }

    private fun com.example.phoenx.data.local.StandaloneMediaEntity.toStandaloneDomain(
        isHeirMode: Boolean,
        activated: Boolean
    ): PhoenXEntry {
        val age = AgeSnapshot(0, 0, 0)
        val needsEncryption = type == "TEXT_EXCERPT" || type == "PHOTO"
        
        val displayTitle = if (isHeirMode && !activated) {
            when(type) {
                "SPOTIFY" -> "Musique scellée"
                "YOUTUBE" -> "Vidéo scellée"
                "PHOTO" -> "Photo scellée"
                "TEXT_EXCERPT" -> "Écrit scellé"
                else -> "Média scellé"
            }
        } else title.ifEmpty { 
            when(type) {
                "SPOTIFY" -> "Morceau partagé"
                "YOUTUBE" -> "Vidéo partagée"
                else -> "Média"
            }
        }

        val domainType = when(type) {
            "SPOTIFY" -> EntryType.AUDIO
            "YOUTUBE" -> EntryType.VIDEO
            "PHOTO" -> EntryType.PHOTO
            "TEXT_EXCERPT" -> EntryType.THOUGHT
            else -> EntryType.THOUGHT
        }

        // Déchiffrement du contenu si nécessaire
        val finalContent = if (isHeirMode && !activated) {
            "Scellé"
        } else if (needsEncryption) {
            try {
                val bytes = android.util.Base64.decode(content, android.util.Base64.DEFAULT)
                encryptionManager.decryptText(bytes, if (isHeirMode) _heirKey.value else null)
            } catch (e: Exception) {
                "Contenu chiffré"
            }
        } else {
            content
        }

        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            ageAtCreation = age,
            encryptedContent = finalContent.toByteArray(),
            type = domainType,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = displayTitle,
            userComment = if (isHeirMode && !activated) null else userComment,
            mediaUrl = if (domainType == EntryType.PHOTO || domainType == EntryType.VIDEO || type == "SPOTIFY") finalContent else null,
            coverUrl = coverUrl,
            localCoverPath = localCoverPath,
            mediaProvider = mediaProvider,
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() }.map { it.trim() }.distinct(),
            visibility = visibility
        )
    }
}
