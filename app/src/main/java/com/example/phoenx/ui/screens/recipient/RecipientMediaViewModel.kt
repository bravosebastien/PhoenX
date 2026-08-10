package com.example.phoenx.ui.screens.recipient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.example.phoenx.ui.theme.AppThemeState
import com.google.firebase.firestore.Blob
import kotlinx.coroutines.channels.awaitClose
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

@Composable
fun MediaViewModeSelector(
    currentMode: MediaViewMode,
    onModeChange: (MediaViewMode) -> Unit,
    filterRecipientId: String?,
    onRecipientChange: (String?) -> Unit,
    recipients: List<RecipientEntity>,
    theme: com.example.phoenx.ui.theme.AppThemeState,
    accent: Color,
    // v9.4.27 : Filtre de contenu (Optionnel, utilisé pour Discothèque)
    currentContentFilter: DiscothequeFilter? = null,
    onContentFilterChange: ((DiscothequeFilter) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentMode == MediaViewMode.DEFAULT,
                    onClick = { onModeChange(MediaViewMode.DEFAULT) },
                    label = { Text("Standard", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = Color.Black)
                )
            }
            item {
                FilterChip(
                    selected = currentMode == MediaViewMode.BY_MEMORY,
                    onClick = { onModeChange(MediaViewMode.BY_MEMORY) },
                    label = { Text("Par Souvenir", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = Color.Black)
                )
            }
            item {
                FilterChip(
                    selected = currentMode == MediaViewMode.BY_RECIPIENT,
                    onClick = { onModeChange(MediaViewMode.BY_RECIPIENT) },
                    label = { Text("Par Destinataire", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = Color.Black)
                )
            }
        }

        // v9.4.27 : Filtre de contenu (Vocaux / Musiques)
        if (currentContentFilter != null && onContentFilterChange != null) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = currentContentFilter == DiscothequeFilter.ALL,
                        onClick = { onContentFilterChange(DiscothequeFilter.ALL) },
                        label = { Text("Tous les audios", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = Color.Black)
                    )
                }
                item {
                    FilterChip(
                        selected = currentContentFilter == DiscothequeFilter.VOCALS,
                        onClick = { onContentFilterChange(DiscothequeFilter.VOCALS) },
                        label = { Text("Vocaux uniquement", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = Color.Black)
                    )
                }
                item {
                    FilterChip(
                        selected = currentContentFilter == DiscothequeFilter.MUSIC,
                        onClick = { onContentFilterChange(DiscothequeFilter.MUSIC) },
                        label = { Text("Musiques uniquement", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = Color.Black)
                    )
                }
            }
        }

        if (currentMode == MediaViewMode.BY_RECIPIENT) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = androidx.compose.ui.Modifier.padding(top = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterRecipientId == null,
                        onClick = { onRecipientChange(null) },
                        label = { Text("Tous", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = Color.Black)
                    )
                }
                items(recipients) { recipient ->
                    FilterChip(
                        selected = filterRecipientId == recipient.linkedUid,
                        onClick = { onRecipientChange(recipient.linkedUid) },
                        label = { Text(recipient.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}

enum class MediaViewMode {
    DEFAULT, BY_MEMORY, BY_RECIPIENT
}

enum class DiscothequeFilter {
    ALL, VOCALS, MUSIC
}

data class ExternalMetadata(
    val title: String? = null,
    val thumbnailUrl: String? = null
)

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

    private val _viewMode = MutableStateFlow(MediaViewMode.DEFAULT)
    val viewMode: StateFlow<MediaViewMode> = _viewMode.asStateFlow()

    private val _discothequeFilter = MutableStateFlow(DiscothequeFilter.ALL)
    val discothequeFilter: StateFlow<DiscothequeFilter> = _discothequeFilter.asStateFlow()

    private val _filterRecipientId = MutableStateFlow<String?>(null) // UID du destinataire
    val filterRecipientId: StateFlow<String?> = _filterRecipientId.asStateFlow()

    fun setViewMode(mode: MediaViewMode) {
        _viewMode.value = mode
    }

    fun setDiscothequeFilter(filter: DiscothequeFilter) {
        _discothequeFilter.value = filter
    }

    fun setFilterRecipient(uid: String?) {
        _filterRecipientId.value = uid
    }

    /**
     * Récupère les métadonnées (titre, miniature) d'un média externe via oEmbed (v9.4.27)
     */
    suspend fun fetchExternalMetadata(url: String): ExternalMetadata? = withContext(Dispatchers.IO) {
        try {
            val endpoint = when {
                url.contains("spotify.com") -> "https://open.spotify.com/oembed?url=$url"
                url.contains("deezer.com") -> "https://api.deezer.com/oembed?url=$url"
                url.contains("youtube.com") || url.contains("youtu.be") -> "https://www.youtube.com/oembed?url=$url&format=json"
                else -> return@withContext null
            }

            val connection = java.net.URL(endpoint).openConnection()
            connection.connect()
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(response)
            
            val rawTitle = json.optString("title")
            val thumbUrl = json.optString("thumbnail_url")

            ExternalMetadata(
                title = if (rawTitle.isNullOrBlank()) null else rawTitle,
                thumbnailUrl = if (thumbUrl.isNullOrBlank()) null else thumbUrl
            )
        } catch (e: Exception) {
            android.util.Log.e("ExternalMetadata", "Erreur oEmbed pour $url", e)
            null
        }
    }

    // Cache des titres de parents pour le groupage (v9.4.27)
    private val _parentTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val parentTitles: StateFlow<Map<String, String>> = _parentTitles.asStateFlow()

    private val _libraryEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val libraryEntries: StateFlow<List<PhoenXEntry>> = _libraryEntries

    private val _discothequeEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val discothequeEntries: StateFlow<List<PhoenXEntry>> = _discothequeEntries

    private val _archiveEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val archiveEntries: StateFlow<List<PhoenXEntry>> = _archiveEntries

    private val _videoEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val videoEntries: StateFlow<List<PhoenXEntry>> = _videoEntries

    val recipientsFlow: StateFlow<List<RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _heritageEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val heritageEntries: StateFlow<List<PhoenXEntry>> = _heritageEntries

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    private val _isProtocolActivated = MutableStateFlow(true)
    val isProtocolActivated: StateFlow<Boolean> = _isProtocolActivated.asStateFlow()

    private val _canAskQuestions = MutableStateFlow(false)
    val canAskQuestions: StateFlow<Boolean> = _canAskQuestions.asStateFlow()

    private val _maxQuestions = MutableStateFlow<Int?>(null)
    val maxQuestions: StateFlow<Int?> = _maxQuestions.asStateFlow()

    private val _questionsAsked = MutableStateFlow(0)
    val questionsAsked: StateFlow<Int> = _questionsAsked.asStateFlow()

    private val _recipientId = MutableStateFlow<String?>(null)
    val recipientId: StateFlow<String?> = _recipientId.asStateFlow()

    private val _bookSealedMessage = MutableStateFlow<String?>(null)
    val bookSealedMessage: StateFlow<String?> = _bookSealedMessage.asStateFlow()

    private val _bookTitle = MutableStateFlow<String?>(null)
    val bookTitle: StateFlow<String?> = _bookTitle.asStateFlow()

    private val _creatorName = MutableStateFlow("Votre proche")
    val creatorName: StateFlow<String> = _creatorName.asStateFlow()

    private val _ambiance = MutableStateFlow(AmbianceState())
    val ambiance: StateFlow<AmbianceState> = _ambiance.asStateFlow()

    private val _targetCreatorId = MutableStateFlow<String?>(null)

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    init {
        loadAllMedia()
        loadParentTitles() // v9.4.27
    }

    private fun loadParentTitles() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collect { entries ->
                val titles = entries.associate { it.id to it.aiSummary }
                _parentTitles.value = titles
            }
        }
    }

    fun setTargetCreator(creatorId: String?) {
        _targetCreatorId.value = creatorId
        if (creatorId != null && creatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                try {
                    // Fetch Creator Name (v8.6.2)
                    val creatorDoc = db.collection("users").document(creatorId).get().await()
                    _creatorName.value = creatorDoc.getString("displayName") ?: "Votre proche"

                    // Charger l'ambiance (v9.4.27)
                    val profDoc = db.collection("users").document(creatorId)
                        .collection("profile").document("creator").get().await()
                    
                    if (profDoc.exists()) {
                        _ambiance.value = AmbianceState(
                            backgroundId = profDoc.getString("transmissionBackgroundId") ?: "classic_ivory",
                            fontId = profDoc.getString("transmissionFontId") ?: "playfair_display"
                        )
                    }

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
                        _maxQuestions.value = recipientDoc.getLong("maxQuestionsAllowed")?.toInt()
                        _questionsAsked.value = recipientDoc.getLong("questionsAskedCount")?.toInt() ?: 0
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
        visibility: String = "RESTRICTED",
        autoThumbUrl: String? = null // v9.4.27
    ) {
        viewModelScope.launch {
            val mediaId = existingId ?: java.util.UUID.randomUUID().toString()
            val needsEncryption = type == "TEXT_EXCERPT" || type == "PHOTO"
            
            // v9.4.27 : Détection auto YouTube si type générique envoyé
            val finalType = if (content.contains("youtube") || content.contains("youtu.be")) "YOUTUBE" else type

            val finalContent = if (needsEncryption) {
                val encrypted = encryptionManager.encryptText(content)
                android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
            } else {
                content
            }

            val entity = com.example.phoenx.data.local.StandaloneMediaEntity(
                id = mediaId,
                creatorUid = currentUid,
                type = finalType,
                title = title,
                userComment = userComment,
                content = finalContent,
                recipientIds = recipientIds.distinct().joinToString(","),
                visibility = visibility,
                createdAt = System.currentTimeMillis(),
                syncStatus = "pending"
            )

            standaloneMediaDao.insertMedia(entity)
            
            // v9.4.27 : Auto-Miniature (YouTube ou oEmbed Thumbnail)
            if (autoThumbUrl != null) {
                fetchAndStoreExternalThumbnail(mediaId, autoThumbUrl)
            } else if (finalType == "YOUTUBE") {
                // Fallback pour YouTube si oEmbed a échoué mais qu'on a l'ID
                extractYouTubeId(content)?.let { id ->
                    fetchAndStoreExternalThumbnail(mediaId, "https://img.youtube.com/vi/$id/hqdefault.jpg")
                }
            }
        }
    }

    private fun extractYouTubeId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#&?\\n]*".toRegex()
        val match = pattern.find(url)
        return match?.value
    }

    private suspend fun fetchAndStoreExternalThumbnail(mediaId: String, thumbnailUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        val context = com.google.firebase.FirebaseApp.getInstance().applicationContext

        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL(thumbnailUrl)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                val tempFile = java.io.File(context.cacheDir, "ext_thumb_$mediaId.jpg")
                
                java.io.FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                // Chiffrement et Upload (Comme une couverture manuelle)
                val storagePath = mediaManager.encryptAndUpload(uid, mediaId, tempFile)
                standaloneMediaDao.updateMediaCover(mediaId, storagePath, tempFile.absolutePath)
                standaloneMediaDao.updateSyncStatus(mediaId, "pending")
                
                android.util.Log.d("ExternalThumb", "Miniature externe auto-récupérée pour $mediaId")
            } catch (e: Exception) {
                android.util.Log.e("ExternalThumb", "Échec récupération miniature externe", e)
            }
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
                    val provider = when {
                        url.contains("deezer") -> "DEEZER"
                        url.contains("youtube") || url.contains("youtu.be") -> "YOUTUBE"
                        else -> "SPOTIFY"
                    }
                    standaloneMediaDao.updateMedia(id, title, comment, url, recipientIds.joinToString(","), visibility)
                    // On s'assure que le type est mis à jour (v9.4.27)
                    standaloneMediaDao.updateMediaType(id, provider)

                    // v9.4.27 : Si YouTube, on tente de récupérer la miniature automatique
                    if (provider == "YOUTUBE") {
                        extractYouTubeId(url)?.let { ytId ->
                            fetchAndStoreExternalThumbnail(id, "https://img.youtube.com/vi/$ytId/hqdefault.jpg")
                        }
                    }
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
            // 1. Flux des entrées classiques (Room ou Firestore)
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

            // 2. Flux des médias isolés (HORS-LIGNE + Firestore v9.4.27)
            val standaloneMediaFlow = _targetCreatorId.flatMapLatest { targetId ->
                val effectiveId = targetId ?: currentUid
                if (effectiveId.isEmpty()) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())

                val isCreator = effectiveId == currentUid

                callbackFlow {
                    // A. Listener Firestore (Source pour Héritier, Réparation pour Créateur)
                    val listener = db.collection("users").document(effectiveId)
                        .collection("standaloneMedia")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) return@addSnapshotListener
                            
                            val firestoreItems = snapshot?.documents?.mapNotNull { doc ->
                                val recIds = (doc.get("recipientIds") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                                
                                if (isCreator || recIds.isEmpty() || recIds.contains(currentUid)) {
                                    val type = doc.getString("type") ?: ""
                                    val needsEncryption = type == "TEXT_EXCERPT" || type == "PHOTO"
                                    val contentStr = if (needsEncryption) {
                                        val blob = doc.get("content") as? com.google.firebase.firestore.Blob
                                        blob?.toBytes()?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) } ?: ""
                                    } else doc.getString("content") ?: ""

                                    val entity = com.example.phoenx.data.local.StandaloneMediaEntity(
                                        id = doc.id,
                                        creatorUid = effectiveId,
                                        type = type,
                                        title = doc.getString("title") ?: "",
                                        userComment = doc.getString("userComment"),
                                        content = contentStr,
                                        recipientIds = recIds.joinToString(","),
                                        visibility = doc.getString("visibility") ?: "RESTRICTED",
                                        createdAt = doc.getLong("createdAt") ?: 0L,
                                        syncStatus = "synced",
                                        coverUrl = doc.getString("coverUrl"),
                                        mediaProvider = doc.getString("mediaProvider")
                                    )
                                    
                                    if (isCreator) {
                                        // Auto-réparation Room sécurisée (v9.4.27) : Préserve les chemins locaux
                                        launch {
                                            val existing = standaloneMediaDao.getMediaById(entity.id)
                                            val repairedEntity = if (existing != null) {
                                                entity.copy(
                                                    localCoverPath = existing.localCoverPath,
                                                    // content peut être un chemin local pour PHOTO standalone
                                                    content = if (existing.content.startsWith("/data/")) existing.content else entity.content
                                                )
                                            } else entity
                                            standaloneMediaDao.insertMedia(repairedEntity)
                                        }
                                    }
                                    entity
                                } else null
                            } ?: emptyList()

                            // Si Héritier, on émet directement Firestore vers l'UI
                            if (!isCreator) {
                                trySend(firestoreItems)
                            }
                        }

                    // B. Si Créateur, la source de vérité pour l'UI est Room
                    val roomJob = if (isCreator) {
                        launch {
                            standaloneMediaDao.getAllStandaloneMedia().collect { items ->
                                trySend(items)
                            }
                        }
                    } else null

                    awaitClose {
                        listener.remove()
                        roomJob?.cancel()
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

                // 4. Indexation par type (Action 2 : Recalcul sur thread de fond)
                val allDecoded = decodedEntries.toList()
                mapOf(
                    "library" to allDecoded.filter { it.parentEntryId == null && (it.type == EntryType.THOUGHT || it.type == EntryType.LEGACY || it.isYoungSelfLetter) },
                    "video" to allDecoded.filter { it.type == EntryType.VIDEO },
                    "audio" to allDecoded.filter { it.type == EntryType.AUDIO },
                    "photo" to allDecoded.filter { it.type == EntryType.PHOTO },
                    "heritage" to allDecoded.filter { it.parentEntryId == null }.sortedByDescending { it.timestamp }
                )
            }
            .flowOn(Dispatchers.Default)
            .collectLatest { result ->
                // Mise à jour de l'UI avec les listes déjà filtrées
                _libraryEntries.value = result["library"] ?: emptyList()
                _videoEntries.value = result["video"] ?: emptyList()
                _discothequeEntries.value = result["audio"] ?: emptyList()
                _archiveEntries.value = result["photo"] ?: emptyList()
                _heritageEntries.value = result["heritage"] ?: emptyList()
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
            mediaProvider = mediaProvider ?: if (domainType == EntryType.AUDIO) "PHOENX" else null, // v9.4.27 : Fallback pour filtres
            recipientIds = recipientIds.split(",").map { it.trim() }.filter { it.isNotBlank() },
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
                "SPOTIFY", "DEEZER" -> "Musique scellée"
                "YOUTUBE" -> "Vidéo scellée"
                "PHOTO" -> "Photo scellée"
                "TEXT_EXCERPT" -> "Écrit scellé"
                else -> "Média scellé"
            }
        } else title.ifEmpty { 
            when(type) {
                "SPOTIFY", "DEEZER" -> "Morceau partagé"
                "YOUTUBE" -> "Vidéo partagée"
                else -> "Média"
            }
        }

        val domainType = when(type) {
            "SPOTIFY", "DEEZER" -> EntryType.AUDIO
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
            mediaUrl = if (domainType == EntryType.PHOTO || domainType == EntryType.VIDEO || type == "SPOTIFY" || type == "DEEZER") finalContent else null,
            coverUrl = coverUrl,
            localCoverPath = localCoverPath,
            mediaProvider = mediaProvider ?: type, // v9.4.27 : Fallback sur type si provider null
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() }.map { it.trim() }.distinct(),
            visibility = visibility
        )
    }
}
