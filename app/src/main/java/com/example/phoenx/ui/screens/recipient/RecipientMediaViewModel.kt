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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class RecipientMediaViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
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

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    private val _targetCreatorId = MutableStateFlow<String?>(null)

    init {
        loadAllMedia()
    }

    fun setTargetCreator(creatorId: String?) {
        _targetCreatorId.value = creatorId
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
                    android.util.Log.e("RecipientMediaVM", "Erreur chargement clé héritage: ${e.message}")
                }
            }
        }
    }

    private fun loadAllMedia() {
        val currentUid = auth.currentUser?.uid ?: ""
        viewModelScope.launch {
            // On récupère d'abord si on est en mode Créateur ou Destinataire
            val isCreator = try {
                val doc = db.collection("users").document(currentUid).get().await()
                doc.getBoolean("isCreator") ?: true
            } catch(e: Exception) { true }

            combine(
                offlineEntryDao.getAllEntries(),
                _targetCreatorId
            ) { allOfflineEntries, targetId ->
                Pair(allOfflineEntries, targetId)
            }.collectLatest { (allOfflineEntries, targetId) ->
                // 1. On sépare parents et compléments
                val parents = allOfflineEntries.filter { it.parentEntryId == null }
                val complements = allOfflineEntries.filter { it.parentEntryId != null }

                // 2. Filtrage par ACCÈS STRICT et par CRÉATEUR CIBLE
                val accessibleParents = if (isCreator) {
                    parents 
                } else {
                    parents.filter { parent ->
                        val isForMe = parent.visibility == "EVERYONE" || parent.recipientIds.split(",").contains(currentUid)
                        val isFromTarget = targetId == null || parent.creatorUid == targetId
                        isForMe && isFromTarget
                    }
                }

                // 3. Conversion en domaine
                val decodedParents = accessibleParents.map { it.toDomain(encryptionManager) }

                // 4. Indexation automatique par type (incluant compléments)
                _libraryEntries.value = decodedParents.filter { parent ->
                    val hasMatch = parent.type == EntryType.THOUGHT || parent.type == EntryType.LEGACY || parent.isYoungSelfLetter ||
                        complements.any { it.parentEntryId == parent.id && it.entryType == "TEXT" }
                    hasMatch
                }

                _videoEntries.value = decodedParents.filter { parent ->
                    val hasMatch = parent.type == EntryType.VIDEO || 
                        complements.any { it.parentEntryId == parent.id && it.entryType == "VIDEO" }
                    hasMatch
                }

                _discothequeEntries.value = decodedParents.filter { parent ->
                    // Maintien du mélange actuel (AUDIO, NIGHT, EMOTION) comme demandé
                    val mainMatches = parent.type == EntryType.AUDIO || parent.type == EntryType.NIGHT_CAPTURE || parent.type == EntryType.EMOTION
                    val compMatches = complements.any { it.parentEntryId == parent.id && (it.entryType == "AUDIO" || it.entryType == "NIGHT" || it.entryType == "EMOTION") }
                    mainMatches || compMatches
                }

                _archiveEntries.value = decodedParents.filter { parent ->
                    val hasMatch = parent.type == EntryType.PHOTO || 
                        complements.any { it.parentEntryId == parent.id && it.entryType == "PHOTO" }
                    hasMatch
                }
            }
        }
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
            "AUDIO" -> EntryType.AUDIO
            "PHOTO" -> EntryType.PHOTO
            "VIDEO" -> EntryType.VIDEO
            "NIGHT" -> EntryType.NIGHT_CAPTURE
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
            parentEntryId = parentEntryId,
            mediaUrl = mediaUrl,
            localMediaPath = localMediaPath
        )
    }
}
