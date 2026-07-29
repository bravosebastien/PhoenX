package com.example.phoenx.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StandaloneMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: StandaloneMediaEntity)

    @Query("SELECT * FROM standalone_media WHERE type = :type ORDER BY createdAt DESC")
    fun getMediaByType(type: String): Flow<List<StandaloneMediaEntity>>

    @Query("SELECT * FROM standalone_media ORDER BY createdAt DESC")
    fun getAllStandaloneMedia(): Flow<List<StandaloneMediaEntity>>

    @Query("SELECT * FROM standalone_media WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<StandaloneMediaEntity>

    @Query("UPDATE standalone_media SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Delete
    suspend fun deleteMedia(media: StandaloneMediaEntity)

    @Query("DELETE FROM standalone_media")
    suspend fun deleteAll()
}
