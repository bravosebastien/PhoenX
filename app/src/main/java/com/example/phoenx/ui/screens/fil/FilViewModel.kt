package com.example.phoenx.ui.screens.fil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.AmendmentEntity
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXAmendment
import com.example.phoenx.domain.model.PhoenXEntry
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.data.local.RecipientEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.phoenx.data.sync.toOfflineEntry
import kotlinx.coroutines.channels.awaitClose
import java.time.Instant
import java.util.*
import javax.inject.Inject
import org.json.JSONObject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FilViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val aiManager: AIManager,
    val mediaManager: com.example.phoenx.data.media.MediaManager,
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    
    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    private val _isProtocolActivated = MutableStateFlow(true)
    val isProtocolActivated: StateFlow<Boolean> = _isProtocolActivated.asStateFlow()

    fun setTargetCreator(creatorId: String?) {
        _targetCreatorId.value = creatorId
        if (creatorId != null && (creatorId != auth.currentUser?.uid)) {
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
                    android.util.Log.e("FilVM", "Erreur chargement clé héritage: ${e.message}")
                    _isProtocolActivated.value = false
                }
            }
        } else {
            _isProtocolActivated.value = true
            _heirKey.value = null
        }
    }

    private val _uiState = MutableStateFlow<FilUiState>(FilUiState(isLoading = true))
    val uiState: StateFlow<FilUiState> = _uiState

    private val _selectedRecipientId = MutableStateFlow<String?>(null)
    val selectedRecipientId: StateFlow<String?> = _selectedRecipientId

    private val _sortByCreationDate = MutableStateFlow(false)
    val sortByCreationDate: StateFlow<Boolean> = _sortByCreationDate

    val recipients: StateFlow<List<RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeEntries()
    }

    private fun observeEntries() {
        val currentUid = auth.currentUser?.uid ?: ""
        viewModelScope.launch {
            _targetCreatorId.flatMapLatest { targetId ->
                if (targetId == null || targetId == currentUid) {
                    // MODE CRÉATEUR : Lecture Room locale (Ma Mémoire)
                    offlineEntryDao.getAllEntries()
                } else {
                    // MODE HÉRITIER : Lecture Firestore directe (v8.5.5)
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
            }.combine(
                combine(_selectedRecipientId, _sortByCreationDate, _isProtocolActivated) { r, s, a -> Triple(r, s, a) }
            ) { offlineEntries, (recipientId, sortByDate, activated) ->
                val targetId = _targetCreatorId.value
                val isHeirMode = targetId != null && targetId != currentUid

                // 1. Filtrage par CRÉATEUR et ACCÈS
                val rootEntries = if (!isHeirMode) {
                    // Mon propre fil : Uniquement mes propres souvenirs Room (Parents)
                    offlineEntries.filter { it.parentEntryId == null }
                } else {
                    // Fil d'un proche : Filtrage accès héritier
                    offlineEntries.filter { entry ->
                        val isRoot = entry.parentEntryId == null
                        val isForMe = entry.visibility == "EVERYONE" || entry.recipientIds.split(",").contains(currentUid)
                        isRoot && isForMe
                    }
                }

                // 2. Filtrage par destinataire (UI)
                val filteredOffline = if (recipientId != null) {
                    rootEntries.filter { 
                        it.recipientIds.split(",").contains(recipientId) || it.visibility == "EVERYONE"
                    }
                } else {
                    rootEntries
                }

                // 3. Décodage et chargement des amendements
                val decodedEntries = filteredOffline.map { entry ->
                    // On ne charge les amendements Room QUE si on est en mode Créateur local
                    val amendments = if (!isHeirMode) {
                        offlineEntryDao.getAmendmentsForEntrySync(entry.id).map { it.toDomain() }
                    } else emptyList()
                    
                    // GESTION SOUVENIR SCELLÉ (v8.5.9)
                    if (isHeirMode && !activated) {
                        entry.toSealedDomain()
                    } else {
                        entry.toDomain(encryptionManager, amendments)
                    }
                }

                // 4. Tri final
                if (sortByDate) {
                    decodedEntries.sortedByDescending { it.timestamp }
                } else {
                    decodedEntries.sortedByDescending { it.ageAtCreation.years }
                }
            }.collect { list ->
                val minAgeVal = if (list.isEmpty()) 0 else list.minOfOrNull { it.ageAtCreation.years } ?: 0
                val maxAgeVal = if (list.isEmpty()) 100 else list.maxOfOrNull { it.ageAtCreation.years } ?: 100
                
                _uiState.value = FilUiState(
                    entries = list,
                    totalCount = list.size,
                    minAge = minAgeVal,
                    maxAge = maxAgeVal,
                    isLoading = false
                )
            }
        }
    }

    fun setRecipientFilter(id: String?) {
        _selectedRecipientId.value = id
    }

    fun toggleSortOrder() {
        _sortByCreationDate.value = !_sortByCreationDate.value
    }

    fun addAmendment(entryId: String, content: String, originalContent: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val evolution = aiManager.analyzeEvolution(originalContent, content)
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                val encrypted = encryptionManager.encryptText(content)

                val amendment = AmendmentEntity(
                    entryId = entryId,
                    encryptedContent = encrypted,
                    ageAtAmendment = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    aiEvolution = evolution
                )
                offlineEntryDao.insertAmendment(amendment)
            } catch (_: Exception) { }
        }
    }

    private fun AmendmentEntity.toDomain(): PhoenXAmendment {
        val ageJson = JSONObject(ageAtAmendment)
        return PhoenXAmendment(
            id = id,
            encryptedContent = encryptedContent,
            ageAtAmendment = AgeSnapshot(ageJson.getInt("years"), ageJson.getInt("months"), ageJson.getInt("days")),
            createdAt = Instant.ofEpochMilli(createdAt)
        )
    }

    private fun OfflineEntry.toSealedDomain(): PhoenXEntry {
        val ageJson = JSONObject(ageAtCreation)
        val age = AgeSnapshot(
            years = ageJson.getInt("years"),
            months = ageJson.getInt("months"),
            days = ageJson.getInt("days")
        )
        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            ageAtCreation = age,
            encryptedContent = "Souvenir scellé".toByteArray(),
            type = when(entryType) {
                "PORTRAIT" -> EntryType.PORTRAIT
                "QUESTION_ANSWER" -> EntryType.QUESTION_ANSWER
                else -> try { EntryType.valueOf(entryType) } catch(e: Exception) { EntryType.THOUGHT }
            },
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary,
            hasEnigma = enigmaQuestion != null,
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() },
            visibility = visibility
        )
    }

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager, amendments: List<PhoenXAmendment>): PhoenXEntry {
        val decryptedText = encryptionManager.decryptText(encryptedPayload)
        
        val ageJson = JSONObject(ageAtCreation)
        val age = AgeSnapshot(
            years = ageJson.getInt("years"),
            months = ageJson.getInt("months"),
            days = ageJson.getInt("days")
        )

        return PhoenXEntry(
            id = id,
            creatorUid = creatorUid,
            ageAtCreation = age,
            encryptedContent = decryptedText.toByteArray(),
            type = when(entryType) {
                "PORTRAIT" -> EntryType.PORTRAIT
                "QUESTION_ANSWER" -> EntryType.QUESTION_ANSWER
                else -> try { EntryType.valueOf(entryType) } catch(e: Exception) { EntryType.THOUGHT }
            },
            isYoungSelfLetter = isYoungSelfLetter,
            targetAge = targetAge,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary,
            aiTags = aiTags.split(",").filter { it.isNotEmpty() },
            amendments = amendments,
            temporalEvolution = if (amendments.isNotEmpty()) {
                // On récupère l'évolution depuis Room si disponible
                // (Ici on utilise une logique simplifiée pour le lien)
                "Évolution stylistique détectée par l'IA"
            } else null,
            hasEnigma = enigmaQuestion != null,
            scheduledDate = scheduledTimestamp?.let { Instant.ofEpochMilli(it) },
            parentEntryId = parentEntryId,
            mediaUrl = mediaUrl,
            localMediaPath = localMediaPath,
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() },
            visibility = visibility
        )
    }
}

data class FilUiState(
    val entries: List<PhoenXEntry> = emptyList(),
    val totalCount: Int = 0,
    val minAge: Int = 0,
    val maxAge: Int = 0,
    val isLoading: Boolean = false
)
