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
    private val functions: FirebaseFunctions,
    private val mediaManager: com.example.phoenx.data.media.MediaManager // v9.7.5
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    private val _heirKey = MutableStateFlow<ByteArray?>(null)

    init {
        // v9.7.4 : Initialisation réactive de l'UID
        viewModelScope.launch {
            _targetCreatorId.value = auth.currentUser?.uid
        }
    }

    private val _remotePersonalities = MutableStateFlow<List<PersonalityEntity>>(emptyList())

    val personalities: StateFlow<List<PersonalityEntity>> = combine(
        _targetCreatorId,
        personalityDao.getAllPersonalities(),
        _remotePersonalities
    ) { targetId, local, remote ->
        val list = if (targetId == null || targetId == auth.currentUser?.uid) local else remote
        android.util.Log.d("PHOENX_SYNC_PERSO", "PersonalitiesViewModel: Lecture des données (${list.size} items)")
        list
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
        val userId = auth.currentUser?.uid ?: return
        // v9.7.5 : Utilisation de NonCancellable pour garantir que la suppression se termine même si l'écran est fermé
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            try {
                android.util.Log.d("PHOENX_PERSO_DB", "SUPPRESSION PERSONNALITÉ lancée: id=${personality.id}, name=${personality.name}")
                
                // 1. Suppression Firestore (Sous-collection média et fichiers Storage associés)
                val mediaSnapshot = firestore.collection("users").document(userId)
                    .collection("personalities").document(personality.id)
                    .collection("media").get().await()
                
                mediaSnapshot.documents.forEach { doc ->
                    val storagePath = doc.getString("mediaPath")
                    if (storagePath != null && !storagePath.startsWith("/")) {
                        // C'est un chemin Storage, on tente de le supprimer
                        try {
                            mediaManager.deleteFile(storagePath)
                        } catch (_: Exception) {}
                    }
                    doc.reference.delete().await()
                }
                android.util.Log.d("PHOENX_PERSO_DB", "Sous-collection média Firestore et Storage nettoyés (${mediaSnapshot.size()} items)")

                // 2. Suppression de la photo principale sur Storage
                if (!personality.mainPhotoPath.startsWith("/")) {
                    try {
                        mediaManager.deleteFile(personality.mainPhotoPath)
                    } catch (_: Exception) {}
                }

                // 3. Suppression Firestore (Document principal)
                firestore.collection("users").document(userId)
                    .collection("personalities").document(personality.id)
                    .delete().await()
                android.util.Log.d("PHOENX_PERSO_DB", "Document principal Firestore supprimé")

                // 4. Suppression Room locale (La cascade gère personality_media)
                personalityDao.deletePersonality(personality)
                
                android.util.Log.d("PHOENX_PERSO_DB", "SUPPRESSION TERMINÉE (Room + Firestore + Storage)")
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_PERSO_DB", "ERREUR SUPPRESSION: ${e.message}", e)
            }
        }
    }

    fun addMedia(personalityId: String, file: java.io.File) {
        viewModelScope.launch {
            android.util.Log.d("PHOENX_PERSO_DB", "ÉCRITURE MÉDIA: parent=$personalityId, path=${file.name}")
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
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            try {
                // 1. Suppression Storage
                if (!media.mediaPath.startsWith("/")) {
                    mediaManager.deleteFile(media.mediaPath)
                }

                // 2. Suppression Firestore
                firestore.collection("users").document(userId)
                    .collection("personalities").document(media.personalityId)
                    .collection("media").document(media.id)
                    .delete().await()

                // 3. Suppression Room locale
                personalityDao.deleteMedia(media)
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_PERSO_DB", "Erreur suppression média isolé", e)
            }
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
        android.util.Log.d("PHOENX_PERSO_TRACE", "getMediaForPersonality appelé pour ID: $personalityId")
        return _targetCreatorId.flatMapLatest { targetId ->
            val isHeir = targetId != null && targetId != auth.currentUser?.uid
            android.util.Log.d("PHOENX_PERSO_TRACE", "Résolution flux média: isHeir=$isHeir, targetId=$targetId")
            
            if (!isHeir) {
                personalityDao.getMediaForPersonality(personalityId).onEach { 
                    android.util.Log.d("PHOENX_PERSO_TRACE", "Données locales reçues: ${it.size} items")
                }
            } else {
                // Load from Firestore
                callbackFlow {
                    android.util.Log.d("PHOENX_PERSO_TRACE", "Ouverture SnapshotListener Firestore pour média")
                    val listener = firestore.collection("users").document(targetId!!)
                        .collection("personalities").document(personalityId)
                        .collection("media").addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("PHOENX_PERSO_TRACE", "Erreur SnapshotListener", error)
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
                            android.util.Log.d("PHOENX_PERSO_TRACE", "Données distantes reçues: ${list.size} items")
                            trySend(list)
                        }
                    awaitClose { listener.remove() }
                }
            }
        }
    }
}
