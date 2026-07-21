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

    private val _bookSealedMessage = MutableStateFlow<String?>(null)
    val bookSealedMessage: StateFlow<String?> = _bookSealedMessage.asStateFlow()

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

                    if (_isProtocolActivated.value) {
                        val keyDoc = db.collection("users").document(creatorId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
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
        }
    }

    private fun loadAllMedia() {
        val currentUid = auth.currentUser?.uid ?: ""
        viewModelScope.launch {
            _targetCreatorId.flatMapLatest { targetId ->
                if (targetId == null || targetId == currentUid) {
                    // MODE CRÉATEUR : Lecture Room locale (Ma Mémoire)
                    offlineEntryDao.getAllEntries()
                } else {
                    // MODE HÉRITIER : Lecture Firestore directe (v8.5.5)
                    // On combine deux requêtes pour gérer le "OU" (visibility OR recipientIds)
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
            }.combine(_isProtocolActivated) { entries, activated ->
                entries to activated
            }.collectLatest { (allOfflineEntries, activated) ->
                val targetId = _targetCreatorId.value
                val isHeirMode = targetId != null && targetId != currentUid

                // 1. On sépare parents et compléments
                val parents = allOfflineEntries.filter { it.parentEntryId == null }
                val complements = allOfflineEntries.filter { it.parentEntryId != null }

                // 2. Filtrage par ACCÈS STRICT
                val accessibleParents = if (!isHeirMode) {
                    parents 
                } else {
                    parents.filter { parent ->
                        val isForMe = parent.visibility == "EVERYONE" || parent.recipientIds.split(",").contains(currentUid)
                        isForMe
                    }
                }

                // 3. Conversion en domaine
                val decodedParents = accessibleParents.map { 
                    if (isHeirMode && !activated) it.toSealedDomain()
                    else it.toDomain(encryptionManager) 
                }

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
                    val mainMatches = parent.type == EntryType.AUDIO || parent.type == EntryType.NIGHT_CAPTURE || parent.type == EntryType.EMOTION
                    val compMatches = complements.any { it.parentEntryId == parent.id && (it.entryType == "AUDIO" || it.entryType == "NIGHT" || it.entryType == "EMOTION") }
                    mainMatches || compMatches
                }

                _archiveEntries.value = decodedParents.filter { parent ->
                    val hasMatch = parent.type == EntryType.PHOTO || 
                        complements.any { it.parentEntryId == parent.id && it.entryType == "PHOTO" }
                    hasMatch
                }

                // 5. Unified Heritage List (v8.5.3)
                _heritageEntries.value = decodedParents.sortedByDescending { it.timestamp }
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
            aiSummary = getString("aiSummary") ?: "",
            aiTags = (get("aiTags") as? List<*>)?.joinToString(",") ?: "",
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
