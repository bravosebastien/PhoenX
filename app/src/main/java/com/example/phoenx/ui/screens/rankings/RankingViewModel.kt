package com.example.phoenx.ui.screens.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.R
import com.example.phoenx.data.local.RankMediaDao
import com.example.phoenx.data.local.RankMediaEntity
import com.example.phoenx.data.local.RankingDao
import com.example.phoenx.data.local.RankingEntity
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.domain.model.Ranking
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.data.sync.toRankingEntity
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class RankMediaItem(
    val id: String,
    val rankingId: String,
    val rankIndex: Int,
    val mediaPath: String,
    val mediaType: String = "PHOTO",
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingDao: RankingDao,
    private val rankMediaDao: RankMediaDao,
    private val mediaManager: MediaManager,
    private val db: FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    private val _remoteRankings = MutableStateFlow<List<Ranking>>(emptyList())
    private val _currentRankingId = MutableStateFlow<String?>(null)
    private val _remoteRankMediaMap = MutableStateFlow<Map<Int, List<RankMediaItem>>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRankings: StateFlow<List<Ranking>> = _targetCreatorId.flatMapLatest { targetId ->
        if (targetId == null) {
            rankingDao.getAllRankings().map { entities -> entities.map { it.toDomain() } }
        } else {
            _remoteRankings
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentRanking: StateFlow<Ranking?> = combine(_currentRankingId, _targetCreatorId, allRankings) { id, targetId, all ->
        if (id == null) null
        else all.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val rankMediaMap: StateFlow<Map<Int, List<RankMediaItem>>> = combine(_currentRankingId, _targetCreatorId) { rId, targetId ->
        Pair(rId, targetId)
    }.flatMapLatest { (rId, targetId) ->
        if (rId == null) {
            flowOf(emptyMap())
        } else if (targetId == null) {
            rankMediaDao.getMediaForRanking(rId).map { list ->
                list.map { entity ->
                    RankMediaItem(
                        id = entity.id,
                        rankingId = entity.rankingId,
                        rankIndex = entity.rankIndex,
                        mediaPath = entity.mediaPath,
                        mediaType = entity.mediaType,
                        thumbnailPath = entity.thumbnailPath,
                        createdAt = entity.createdAt
                    )
                }.groupBy { it.rankIndex }
            }
        } else {
            _remoteRankMediaMap
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setRankingId(id: String?) {
        _currentRankingId.value = id
        val targetId = _targetCreatorId.value
        if (id != null && targetId != null) {
            loadRemoteRankMedia(targetId, id)
        }
    }

    fun setTargetCreator(creatorId: String?) {
        _targetCreatorId.value = creatorId
        if (creatorId != null) {
            loadRemoteRankings(creatorId)
            val rId = _currentRankingId.value
            if (rId != null) {
                loadRemoteRankMedia(creatorId, rId)
            }
        }
    }

    private fun loadRemoteRankings(creatorId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(creatorId)
                    .collection("rankings").get().await()
                val list = snapshot.documents.map { it.toRankingEntity().toDomain() }
                _remoteRankings.value = list
            } catch (e: Exception) {
                android.util.Log.e("RankingVM", "Error loading remote: ${e.message}")
            }
        }
    }

    private fun loadRemoteRankMedia(creatorId: String, rankingId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(creatorId)
                    .collection("rankings").document(rankingId)
                    .collection("rankMedia").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val rId = doc.getString("rankingId") ?: rankingId
                    val index = doc.getLong("rankIndex")?.toInt() ?: return@mapNotNull null
                    val path = doc.getString("mediaPath") ?: return@mapNotNull null
                    val type = doc.getString("mediaType") ?: "PHOTO"
                    val thumbPath = doc.getString("thumbnailPath")
                    val created = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    RankMediaItem(id, rId, index, path, type, thumbPath, created)
                }
                _remoteRankMediaMap.value = list.groupBy { it.rankIndex }
            } catch (e: Exception) {
                android.util.Log.e("RankingVM", "Error loading remote rank media: ${e.message}")
            }
        }
    }

    fun attachRankMedia(rankingId: String, rankIndex: Int, file: File, isVideo: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Garde-fou : Limite de 5 médias maximum par rang
                val currentMediaList = rankMediaMap.value[rankIndex] ?: emptyList()
                if (currentMediaList.size >= 5) {
                    android.util.Log.w("RankingVM", "Alerte : Limite de 5 médias atteinte pour le rang $rankIndex")
                    return@launch
                }

                var thumbnailStoragePath: String? = null

                if (isVideo) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(file.absolutePath)
                        val bitmap = retriever.getFrameAtTime(0)
                        retriever.release()

                        if (bitmap != null) {
                            val thumbFile = File(context.cacheDir, "thumb_${file.name}.jpg")
                            java.io.FileOutputStream(thumbFile).use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
                            }
                            thumbnailStoragePath = mediaManager.uploadRankMediaThumbnail(uid, rankingId, rankIndex, thumbFile)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RankingVM", "Erreur vignette vidéo: ${e.message}")
                    }
                }

                // 1. Upload du vidéo/photo principal vers Storage
                val storagePath = mediaManager.uploadRankMedia(uid, rankingId, rankIndex, file, isVideo)

                // 2. Insertion Room
                val mediaId = UUID.randomUUID().toString()
                val mediaTypeStr = if (isVideo) "VIDEO" else "PHOTO"
                val entity = RankMediaEntity(
                    id = mediaId,
                    rankingId = rankingId,
                    rankIndex = rankIndex,
                    mediaPath = storagePath,
                    mediaType = mediaTypeStr,
                    thumbnailPath = thumbnailStoragePath,
                    syncStatus = "synced"
                )
                rankMediaDao.upsertRankMedia(entity)

                // 3. Écriture Firestore sous users/{uid}/rankings/{rankingId}/rankMedia/{mediaId}
                val firestoreData = mutableMapOf<String, Any?>(
                    "id" to mediaId,
                    "rankingId" to rankingId,
                    "rankIndex" to rankIndex,
                    "mediaPath" to storagePath,
                    "mediaType" to mediaTypeStr,
                    "createdAt" to System.currentTimeMillis()
                )
                if (thumbnailStoragePath != null) {
                    firestoreData["thumbnailPath"] = thumbnailStoragePath
                }

                db.collection("users").document(uid)
                    .collection("rankings").document(rankingId)
                    .collection("rankMedia").document(mediaId)
                    .set(firestoreData).await()
            } catch (e: Exception) {
                android.util.Log.e("RankingVM", "Error attaching rank media: ${e.message}", e)
            }
        }
    }

    fun removeRankMedia(rankingId: String, mediaId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                try {
                    db.collection("users").document(uid)
                        .collection("rankings").document(rankingId)
                        .collection("rankMedia").document(mediaId).delete().await()
                } catch (_: Exception) {}
                rankMediaDao.deleteRankMediaById(mediaId)
            } catch (e: Exception) {
                android.util.Log.e("RankingVM", "Error removing rank media: ${e.message}", e)
            }
        }
    }

    fun createRanking(title: String, itemCount: Int) {
        viewModelScope.launch {
            val safeTitle = title.replace("|", "")
            val emptyItems = List(itemCount) { "" }.joinToString("|")
            val entity = RankingEntity(
                title = safeTitle,
                itemCount = itemCount,
                items = emptyItems,
                syncStatus = "pending"
            )
            rankingDao.upsertRanking(entity)
            SyncWorker.trigger(context)
        }
    }

    fun updateRankingTitle(id: String, newTitle: String) {
        viewModelScope.launch {
            val existing = rankingDao.getRankingById(id) ?: return@launch
            val safeTitle = newTitle.replace("|", "")
            rankingDao.upsertRanking(existing.copy(
                title = safeTitle,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "pending"
            ))
            SyncWorker.trigger(context)
        }
    }

    fun updateRankItem(id: String, index: Int, text: String) {
        viewModelScope.launch {
            val existing = rankingDao.getRankingById(id) ?: return@launch
            val items = existing.items.split("|").toMutableList()
            
            while (items.size < existing.itemCount) {
                items.add("")
            }

            if (index in 0 until existing.itemCount) {
                val safeText = text.replace("|", "")
                items[index] = safeText
                rankingDao.upsertRanking(existing.copy(
                    items = items.joinToString("|"),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "pending"
                ))
                SyncWorker.trigger(context)
            }
        }
    }

    fun clearRankItem(id: String, index: Int) {
        updateRankItem(id, index, "")
    }

    fun updateCoverImage(id: String, path: String) {
        viewModelScope.launch {
            val existing = rankingDao.getRankingById(id) ?: return@launch
            rankingDao.upsertRanking(existing.copy(
                coverImageUrl = path,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "pending"
            ))
            SyncWorker.trigger(context)
        }
    }

    fun deleteRanking(id: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val existing = rankingDao.getRankingById(id) ?: return@launch
            
            // 1. Suppression Storage si image de couverture existe
            if (!existing.coverImageUrl.isNullOrBlank()) {
                try {
                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(existing.coverImageUrl)
                    storageRef.delete().await()
                } catch (e: Exception) {
                    android.util.Log.e("RankingVM", "Error deleting from Storage: ${e.message}")
                }
            }

            // 2. Suppression Firestore
            try {
                db.collection("users").document(userId)
                    .collection("rankings").document(id)
                    .delete().await()
            } catch (e: Exception) {
                android.util.Log.e("RankingVM", "Error deleting from Firestore: ${e.message}")
            }

            // 3. Suppression Room
            rankMediaDao.deleteRankMediaForRanking(id)
            rankingDao.deleteRanking(existing)

            SyncWorker.trigger(context)
        }
    }

    private fun RankingEntity.toDomain(): Ranking {
        val itemList = if (items.isEmpty()) List(itemCount) { "" } else items.split("|")
        val fixedList = if (itemList.size == itemCount) itemList 
                        else List(itemCount) { i -> itemList.getOrElse(i) { "" } }
                        
        return Ranking(
            id = id,
            title = title,
            itemCount = itemCount,
            items = fixedList,
            coverImageUrl = coverImageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
