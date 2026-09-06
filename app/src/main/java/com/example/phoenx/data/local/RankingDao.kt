package com.example.phoenx.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RankingDao {
    @Query("SELECT * FROM rankings ORDER BY createdAt DESC")
    fun getAllRankings(): Flow<List<RankingEntity>>

    @Query("SELECT * FROM rankings WHERE id = :id")
    suspend fun getRankingById(id: String): RankingEntity?

    @Query("SELECT * FROM rankings WHERE id = :id")
    fun getRankingByIdFlow(id: String): Flow<RankingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRanking(ranking: RankingEntity)

    @Delete
    suspend fun deleteRanking(ranking: RankingEntity)

    @Query("SELECT * FROM rankings WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<RankingEntity>
}
