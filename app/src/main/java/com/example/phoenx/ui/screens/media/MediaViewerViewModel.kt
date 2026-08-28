package com.example.phoenx.ui.screens.media

import android.util.Log
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: StandaloneMediaDao,
    private val encryptionManager: EncryptionManager,
    val mediaManager: MediaManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _entryId = MutableStateFlow<String?>(null)
    private val _creatorId = MutableStateFlow<String?>(null)
    private val _heirKey = MutableStateFlow<ByteArray?>(null)

    private val _manualMediaUrl = MutableStateFlow<String?>(null)
    private val _manualEntryType = MutableStateFlow<String?>(null)
    private val _manualAiSummary = MutableStateFlow<String?>(null)
    private val _manualSourceDocType = MutableStateFlow<String?>(null)
    private val _manualPersonId = MutableStateFlow<String?>(null)
    private val _manualIsEncrypted = MutableStateFlow<Boolean>(true)

    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    init {
        Log.d("PHX_MEDIA_DEBUG", "ViewModel INIT: hash=${System.identityHashCode(this)}")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("PHX_MEDIA_DEBUG", "ViewModel CLEARED: hash=${System.identityHashCode(this)}")
    }

    /**
     * Source de vérité hybride et stable (v9.4.27)
     * Supporte la transmission directe des données pour éviter les échecs de résolution d'ID.
     */
    val entry: StateFlow<PhoenXEntry?> = combine(
        _entryId, _creatorId, _heirKey, 
        _manualMediaUrl, _manualEntryType, _manualAiSummary, _manualSourceDocType, _manualPersonId, _manualIsEncrypted
    ) { args: Array<Any?> ->
        val id = args[0] as? String
        val cId = args[1] as? String
        val key = args[2] as? ByteArray
        val mUrl = args[3] as? String
        val mType = args[4] as? String
        val mSum = args[5] as? String
        val mDocType = args[6] as? String
        val mPersonId = args[7] as? String
        val mIsEncrypted = args[8] as? Boolean ?: true

        Log.d("PHX_MEDIA_DEBUG", "COMBINE TICK: hash=${System.identityHashCode(this)} id=$id mUrl=${mUrl != null}")

        if (id == null) return@combine null

        val isRecipient = cId != null && cId != auth.currentUser?.uid
        
        // GATING : On attend la clé si on est destinataire
        if (isRecipient && key == null) {
            Log.d("PHX_MEDIA_DEBUG", "GATING: Waiting for heir key for id=$id")
            return@combine null
        }

        // OPTION 2 : Transmission directe (Prioritaire)
        if (mUrl != null && mType != null) {
            val domainType = when(mType) {
                "PHOTO", "GALLERY" -> EntryType.PHOTO
                "AUDIO" -> EntryType.AUDIO
                "VIDEO" -> EntryType.VIDEO
                else -> EntryType.THOUGHT
            }
            Log.d("PHX_MEDIA_DEBUG", "COMBINE SUCCESS (Direct): id=$id type=$domainType")
            return@combine PhoenXEntry(
                id = id,
                creatorUid = cId ?: "",
                ageAtCreation = AgeSnapshot(0,0,0),
                encryptedContent = ByteArray(0),
                type = domainType,
                timestamp = Instant.now(),
                aiSummary = mSum ?: "",
                mediaUrl = mUrl,
                sourceDocType = mDocType ?: "entries",
                personId = mPersonId,
                isEncrypted = mIsEncrypted
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
        sourceDocType: String? = null,
        personId: String? = null,
        isEncrypted: Boolean = true
    ) {
        Log.d("PHX_MEDIA_DEBUG", "loadMedia START: hash=${System.identityHashCode(this)} id=$entryId")
        
        // Reset pour forcer le combine (Fix Bug 3)
        _entryId.value = null
        Log.d("PHX_MEDIA_DEBUG", "loadMedia: _entryId RESET to null")
        
        val cleanCreatorId = creatorId?.takeIf { it.isNotBlank() && !it.startsWith("{") && it != "null" }
        
        _entryId.value = entryId
        _creatorId.value = cleanCreatorId
        _manualMediaUrl.value = mediaUrl
        _manualEntryType.value = entryType
        _manualAiSummary.value = aiSummary
        _manualSourceDocType.value = sourceDocType
        _manualPersonId.value = personId
        _manualIsEncrypted.value = isEncrypted
        Log.d("PHX_MEDIA_DEBUG", "loadMedia END: _entryId SET to $entryId")

        if (cleanCreatorId != null && cleanCreatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                try {
                    val keyDoc = db.collection("users").document(cleanCreatorId)
                        .collection("entry_keys").document("main").get().await()
                    
                    val keyBase64 = keyDoc.getString("key")
                    if (keyBase64 != null) {
                        _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaViewerVM", "Erreur chargement clé héritage: ${e.message}")
                }
            }
        }
    }

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager): PhoenXEntry {
        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            timestamp = Instant.ofEpochMilli(createdAt),
            ageAtCreation = AgeSnapshot(0, 0, 0),
            encryptedContent = encryptedPayload,
            type = when(entryType) {
                "PHOTO" -> EntryType.PHOTO
                "VIDEO" -> EntryType.VIDEO
                "AUDIO" -> EntryType.AUDIO
                else -> EntryType.THOUGHT
            },
            aiSummary = aiSummary,
            mediaUrl = mediaUrl,
            localMediaPath = localMediaPath
        )
    }

    private fun StandaloneMediaEntity.toStandaloneDomain(): PhoenXEntry {
        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            timestamp = Instant.ofEpochMilli(createdAt),
            ageAtCreation = AgeSnapshot(0, 0, 0),
            encryptedContent = ByteArray(0),
            type = when(type) {
                "PHOTO" -> EntryType.PHOTO
                "VIDEO" -> EntryType.VIDEO
                "AUDIO" -> EntryType.AUDIO
                else -> EntryType.THOUGHT
            },
            aiSummary = title,
            mediaUrl = content,
            sourceDocType = "standaloneMedia"
        )
    }
}
