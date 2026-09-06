package com.example.phoenx.ui.screens.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.RankingDao
import com.example.phoenx.data.local.RankingEntity
import com.example.phoenx.domain.model.Ranking
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.data.sync.toRankingEntity
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingDao: RankingDao,
    private val db: FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    private val _remoteRankings = MutableStateFlow<List<Ranking>>(emptyList())
    private val _currentRankingId = MutableStateFlow<String?>(null)

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

    fun setRankingId(id: String?) {
        _currentRankingId.value = id
    }

    fun setTargetCreator(creatorId: String?) {
        _targetCreatorId.value = creatorId
        if (creatorId != null) {
            loadRemoteRankings(creatorId)
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
                android.util.Log.e("RankingVM", "Erreur chargement remote: ${e.message}")
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
            
            // Si la liste est plus courte que prévue (cas de migration ou erreur), on l'étend
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
            
            // 1. Suppression Storage si image existe
            if (!existing.coverImageUrl.isNullOrBlank()) {
                try {
                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(existing.coverImageUrl)
                    storageRef.delete().await()
                } catch (e: Exception) {
                    android.util.Log.e("RankingVM", "Erreur suppression Storage: ${e.message}")
                }
            }

            // 2. Suppression Firestore
            try {
                db.collection("users").document(userId)
                    .collection("rankings").document(id)
                    .delete().await()
            } catch (e: Exception) {
                android.util.Log.e("RankingVM", "Erreur suppression Firestore: ${e.message}")
            }

            // 3. Suppression Room
            rankingDao.deleteRanking(existing)

            SyncWorker.trigger(context)
        }
    }

    private fun RankingEntity.toDomain(): Ranking {
        val itemList = if (items.isEmpty()) List(itemCount) { "" } else items.split("|")
        // S'assurer que la liste a la bonne taille
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
