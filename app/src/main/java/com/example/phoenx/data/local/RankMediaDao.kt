package com.example.phoenx.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RankMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRankMedia(media: RankMediaEntity)

    @Query("SELECT * FROM rank_media WHERE rankingId = :rankingId ORDER BY rankIndex ASC")
    fun getMediaForRanking(rankingId: String): Flow<List<RankMediaEntity>>

    @Query("SELECT * FROM rank_media WHERE rankingId = :rankingId AND rankIndex = :rankIndex LIMIT 1")
    suspend fun getMediaForRank(rankingId: String, rankIndex: Int): RankMediaEntity?

    @Query("DELETE FROM rank_media WHERE rankingId = :rankingId AND rankIndex = :rankIndex")
    suspend fun deleteRankMedia(rankingId: String, rankIndex: Int)

    @Query("DELETE FROM rank_media WHERE rankingId = :rankingId")
    suspend fun deleteRankMediaForRanking(rankingId: String)

    @Query("SELECT * FROM rank_media WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<RankMediaEntity>

    @Query("UPDATE rank_media SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
