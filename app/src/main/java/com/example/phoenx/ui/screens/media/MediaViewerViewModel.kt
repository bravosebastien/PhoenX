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

    /**
     * Source de vérité hybride (v9.4.27)
     */
    /**
     * Source de vérité hybride (v9.4.27)
     * Gère les souvenirs (parents ou compléments) et les standalone.
     */
    val entry: StateFlow<PhoenXEntry?> = _entryId
        .filterNotNull()
        .flatMapLatest { id ->
            android.util.Log.d("MediaSupportDiag", "Recherche média ID: $id")
            combine(
                offlineEntryDao.getEntryById(id),
                offlineEntryDao.getComplements(id), // On charge aussi les compléments si c'est un parent
                standaloneMediaDao.getAllStandaloneMedia()
            ) { offline, complements, standalones ->
                if (offline != null) {
                    val domain = offline.toDomain(encryptionManager)
                    
                    // Si c'est un parent THOUGHT mais avec des compléments média, 
                    // on "l'enrichit" avec le premier média pour le viewer (v9.4.27)
                    if (domain.type == EntryType.THOUGHT && complements.isNotEmpty()) {
                        val firstMedia = complements.firstOrNull { it.entryType != "TEXT" }
                        if (firstMedia != null) {
                            android.util.Log.d("MediaSupportDiag", "Parent THOUGHT enrichi par complément: ${firstMedia.entryType}")
                            return@combine domain.copy(
                                type = when(firstMedia.entryType) {
                                    "PHOTO", "GALLERY" -> EntryType.PHOTO
                                    "AUDIO" -> EntryType.AUDIO
                                    "VIDEO" -> EntryType.VIDEO
                                    else -> EntryType.THOUGHT
                                },
                                mediaUrl = firstMedia.mediaUrl,
                                localMediaPath = firstMedia.localMediaPath
                            )
                        }
                    }
                    
                    android.util.Log.d("MediaSupportDiag", "Trouvé dans OfflineEntry (Type final: ${domain.type})")
                    domain
                } else {
                    val found = standalones.find { it.id == id }
                    if (found != null) {
                        android.util.Log.d("MediaSupportDiag", "Trouvé dans StandaloneMedia (Type: ${found.type})")
                        found.toStandaloneDomain()
                    } else {
                        android.util.Log.d("MediaSupportDiag", "Média NON TROUVÉ")
                        null
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    fun loadMedia(entryId: String, creatorId: String?) {
        _entryId.value = entryId
        _creatorId.value = creatorId

        if (creatorId != null && creatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                try {
                    val keyDoc = db.collection("users").document(creatorId)
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
            "AUDIO" -> EntryType.AUDIO
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
            mediaUrl = mediaUrl,
            localMediaPath = localMediaPath
        )
    }

    private fun StandaloneMediaEntity.toStandaloneDomain(): PhoenXEntry {
        val domainType = when(type) {
            "PHOTO" -> EntryType.PHOTO
            "SPOTIFY" -> EntryType.AUDIO
            "YOUTUBE" -> EntryType.VIDEO
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
            mediaUrl = content,
            visibility = visibility
        )
    }
}
