package com.example.phoenx.ui.screens.personalities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.PersonalityDao
import com.example.phoenx.data.local.PersonalityEntity
import com.example.phoenx.data.local.PersonalityMediaEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PersonalitiesViewModel @Inject constructor(
    private val personalityDao: PersonalityDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    private val _heirKey = MutableStateFlow<ByteArray?>(null)

    private val _remotePersonalities = MutableStateFlow<List<PersonalityEntity>>(emptyList())

    val personalities: StateFlow<List<PersonalityEntity>> = combine(
        _targetCreatorId,
        personalityDao.getAllPersonalities(),
        _remotePersonalities
    ) { targetId, local, remote ->
        if (targetId == null || targetId == auth.currentUser?.uid) local else remote
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    val filteredPersonalities: StateFlow<List<PersonalityEntity>> = combine(
        personalities,
        _categoryFilter
    ) { list, filter ->
        if (filter == null) list else list.filter { it.category == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTargetCreator(creatorId: String?, heirKey: ByteArray?) {
        _targetCreatorId.value = creatorId
        _heirKey.value = heirKey
        if (creatorId != null && creatorId != auth.currentUser?.uid) {
            loadRemotePersonalities(creatorId)
        }
    }

    private fun loadRemotePersonalities(creatorId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(creatorId)
                    .collection("personalities").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    PersonalityEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "Autre",
                        customCategoryLabel = doc.getString("customCategoryLabel"),
                        mainPhotoPath = doc.getString("mainPhotoPath") ?: "",
                        biography = doc.getString("biography") ?: "",
                        personalComment = doc.getString("personalComment") ?: "",
                        syncStatus = "synced"
                    )
                }
                _remotePersonalities.value = list
            } catch (e: Exception) {
                android.util.Log.e("PersonalitiesVM", "Error loading remote personalities", e)
            }
        }
    }

    fun updateCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun savePersonality(personality: PersonalityEntity) {
        viewModelScope.launch {
            personalityDao.insertPersonality(personality.copy(syncStatus = "pending"))
            // v9.7.1 : Déclenchement forcé de la synchro
            com.example.phoenx.data.sync.SyncWorker.trigger(firestore.app.applicationContext)
        }
    }

    fun deletePersonality(personality: PersonalityEntity) {
        viewModelScope.launch {
            personalityDao.deletePersonality(personality)
            // Handle Firestore deletion
        }
    }

    fun addMedia(personalityId: String, file: java.io.File) {
        viewModelScope.launch {
            val entity = PersonalityMediaEntity(
                id = UUID.randomUUID().toString(),
                personalityId = personalityId,
                mediaPath = file.absolutePath,
                capturedAt = System.currentTimeMillis(),
                syncStatus = "pending"
            )
            personalityDao.insertMedia(entity)
        }
    }

    fun removeMedia(media: PersonalityMediaEntity) {
        viewModelScope.launch {
            personalityDao.deleteMedia(media)
        }
    }

    suspend fun checkContent(text: String): Pair<Boolean, String?> {
        return try {
            val result = functions.getHttpsCallable("checkPersonalityContent")
                .call(mapOf("text" to text)).await()
            val data = result.data as Map<*, *>
            val isSafe = data["isSafe"] as? Boolean ?: true
            val reason = data["reason"] as? String
            isSafe to reason
        } catch (e: Exception) {
            true to null // Fail safe
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getMediaForPersonality(personalityId: String): Flow<List<PersonalityMediaEntity>> {
        return _targetCreatorId.flatMapLatest { targetId ->
            if (targetId == null || targetId == auth.currentUser?.uid) {
                personalityDao.getMediaForPersonality(personalityId)
            } else {
                // Load from Firestore
                callbackFlow {
                    val listener = firestore.collection("users").document(targetId)
                        .collection("personalities").document(personalityId)
                        .collection("media").addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("PersonalitiesVM", "Error listening to media", error)
                                return@addSnapshotListener
                            }
                            val list = snapshot?.documents?.mapNotNull { doc ->
                                PersonalityMediaEntity(
                                    id = doc.id,
                                    personalityId = personalityId,
                                    mediaPath = doc.getString("mediaPath") ?: "",
                                    syncStatus = "synced"
                                )
                            } ?: emptyList()
                            trySend(list)
                        }
                    awaitClose { listener.remove() }
                }
            }
        }
    }
}
