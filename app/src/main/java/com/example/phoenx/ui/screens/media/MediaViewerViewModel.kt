package com.example.phoenx.ui.screens.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.StandaloneMediaDao
import com.example.phoenx.data.local.StandaloneMediaEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: StandaloneMediaDao, // v9.4.27
    private val encryptionManager: EncryptionManager,
    val mediaManager: MediaManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _entryId = MutableStateFlow<String?>(null)
    private val _creatorId = MutableStateFlow<String?>(null)
    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    
    // Paramètres transmis directement (v9.4.27)
    private val _manualMediaUrl = MutableStateFlow<String?>(null)
    private val _manualEntryType = MutableStateFlow<String?>(null)
    private val _manualAiSummary = MutableStateFlow<String?>(null)
    private val _manualSourceDocType = MutableStateFlow<String?>(null)

    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    /**
     * Source de vérité hybride et stable (v9.4.27)
     * Supporte la transmission directe des données pour éviter les échecs de résolution d'ID.
     */
    val entry: StateFlow<PhoenXEntry?> = combine(
        _entryId, _creatorId, _heirKey, 
        _manualMediaUrl, _manualEntryType, _manualAiSummary, _manualSourceDocType
    ) { args: Array<Any?> ->
        val id = args[0] as? String
        val cId = args[1] as? String
        val key = args[2] as? ByteArray
        val mUrl = args[3] as? String
        val mType = args[4] as? String
        val mSum = args[5] as? String
        val mDocType = args[6] as? String

        android.util.Log.d("MediaViewerVM", "COMBINE TICK: id=$id cId=$cId keyPresent=${key != null} manualUrl=${mUrl != null}")

        val isRecipient = cId != null && cId != auth.currentUser?.uid
        
        // GATING : On attend la clé si on est destinataire
        if (isRecipient && key == null) return@combine null

        // OPTION 2 : Transmission directe (Prioritaire)
        if (id != null && mUrl != null && mType != null) {
            val domainType = when(mType) {
                "PHOTO", "GALLERY" -> EntryType.PHOTO
                "AUDIO" -> EntryType.AUDIO
                "VIDEO" -> EntryType.VIDEO
                else -> EntryType.THOUGHT
            }
            return@combine PhoenXEntry(
                id = id,
                creatorUid = cId ?: "",
                ageAtCreation = AgeSnapshot(0,0,0),
                encryptedContent = ByteArray(0),
                type = domainType,
                timestamp = Instant.now(),
                aiSummary = mSum ?: "",
                mediaUrl = mUrl,
                sourceDocType = mDocType ?: "entries"
            )
        }
        
        // Sinon, on laisse le flatMapLatest gérer la résolution via Room (Ancien mécanisme)
        id
    }
    .flatMapLatest { resultOrId ->
        when (resultOrId) {
            null -> flowOf(null)
            is PhoenXEntry -> flowOf(resultOrId)
            is String -> {
                val id = resultOrId
                combine(
                    offlineEntryDao.getEntryById(id),
                    offlineEntryDao.getComplements(id),
                    standaloneMediaDao.getAllStandaloneMedia()
                ) { offline, complements, standalones ->
                    if (offline != null) {
                        val domain = offline.toDomain(encryptionManager)
                        if (domain.type == EntryType.THOUGHT && complements.isNotEmpty()) {
                            val firstMedia = complements.firstOrNull { it.entryType != "TEXT" }
                            if (firstMedia != null) {
                                domain.copy(
                                    type = when(firstMedia.entryType) {
                                        "PHOTO", "GALLERY" -> EntryType.PHOTO
                                        "AUDIO" -> EntryType.AUDIO
                                        "VIDEO" -> EntryType.VIDEO
                                        else -> EntryType.THOUGHT
                                    },
                                    mediaUrl = firstMedia.mediaUrl,
                                    localMediaPath = firstMedia.localMediaPath
                                )
                            } else domain
                        } else domain
                    } else {
                        standalones.find { it.id == id }?.toStandaloneDomain()
                    }
                }
            }
            else -> flowOf(null)
        }
    }
    .distinctUntilChanged { old, new ->
        old?.id == new?.id && old?.mediaUrl == new?.mediaUrl && old?.localMediaPath == new?.localMediaPath
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadMedia(
        entryId: String, 
        creatorId: String?,
        mediaUrl: String? = null,
        entryType: String? = null,
        aiSummary: String? = null,
        sourceDocType: String? = null
    ) {
        android.util.Log.d("MediaViewerVM", "Instance loadMedia = ${System.identityHashCode(this)}")
        android.util.Log.d("MediaSupportDiag", "loadMedia: entryId=$entryId, manualUrl=${mediaUrl != null}")
        
        val cleanCreatorId = creatorId?.takeIf { it.isNotBlank() && !it.startsWith("{") && it != "null" }
        
        _entryId.value = entryId
        _creatorId.value = cleanCreatorId
        _manualMediaUrl.value = mediaUrl
        _manualEntryType.value = entryType
        _manualAiSummary.value = aiSummary
        _manualSourceDocType.value = sourceDocType

        if (cleanCreatorId != null && cleanCreatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                try {
                    android.util.Log.d("MediaViewerVM", "Lancement requête entry_keys pour creatorId=$creatorId")
                    val keyDoc = db.collection("users").document(creatorId!!)
                        .collection("entry_keys").document("main").get().await()
                    
                    android.util.Log.d("MediaViewerVM", "Requête terminée, exists=${keyDoc.exists()}")
                    android.util.Log.d("MediaViewerVM", "Contenu brut du document: ${keyDoc.data}")
                    val keyBase64 = keyDoc.getString("key")
                    if (keyBase64 != null) {
                        _heirKey.value = android.util.Base64.decode(keyBase64 as String, android.util.Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaViewerVM", "Erreur chargement clé héritage: ${e.message}")
                }
            }
        }
    }

    // --- MAPPERS INTERNES (v9.4.27) ---

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager): PhoenXEntry {
        val decryptedText = try { 
            encryptionManager.decryptText(encryptedPayload)
        } catch(_: Exception) { "Contenu chiffré" }
        
        // Parsing simplifié de l'âge
        val age = try {
            val json = org.json.JSONObject(ageAtCreation)
            AgeSnapshot(json.getInt("years"), json.getInt("months"), json.getInt("days"))
        } catch(_: Exception) { AgeSnapshot(0,0,0) }

        val domainType = when(entryType) {
            "PHOTO", "GALLERY" -> EntryType.PHOTO // v9.4.27 : Restauration GALLERY
            "AUDIO", "EMOTION" -> EntryType.AUDIO
            "VIDEO" -> EntryType.VIDEO
            else -> EntryType.THOUGHT
        }

        android.util.Log.d("MediaSupportDiag", "toDomain (Offline) - Original Type: $entryType -> Domain Type: $domainType")

        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            ageAtCreation = age,
            encryptedContent = decryptedText.toByteArray(),
            type = domainType,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary,
            userComment = userComment,
            mediaUrl = mediaUrl,
            localMediaPath = localMediaPath,
            coverUrl = coverUrl,
            localCoverPath = localCoverPath,
            mediaProvider = mediaProvider,
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() },
            visibility = visibility
        )
    }

    private fun StandaloneMediaEntity.toStandaloneDomain(): PhoenXEntry {
        val domainType = when(type) {
            "PHOTO" -> EntryType.PHOTO
            "SPOTIFY", "DEEZER" -> EntryType.AUDIO
            "YOUTUBE", "VIDEO" -> EntryType.VIDEO
            else -> EntryType.THOUGHT
        }

        android.util.Log.d("MediaSupportDiag", "toStandaloneDomain - Original Type: $type -> Domain Type: $domainType")

        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            ageAtCreation = AgeSnapshot(0,0,0),
            encryptedContent = content.toByteArray(),
            type = domainType,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = title,
            userComment = userComment,
            mediaUrl = content,
            coverUrl = coverUrl,
            localCoverPath = localCoverPath,
            mediaProvider = mediaProvider,
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() }.map { it.trim() }.distinct(),
            visibility = visibility,
            sourceDocType = "standaloneMedia" // v9.4.27
        )
    }
}
