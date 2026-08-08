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

    @Query("SELECT * FROM standalone_media WHERE id = :id")
    suspend fun getMediaById(id: String): StandaloneMediaEntity?

    @Query("SELECT * FROM standalone_media WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<StandaloneMediaEntity>

    @Query("UPDATE standalone_media SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE standalone_media SET title = :title, userComment = :userComment, content = :url, recipientIds = :recipientIds, visibility = :visibility WHERE id = :id")
    suspend fun updateMedia(id: String, title: String, userComment: String?, url: String, recipientIds: String, visibility: String)

    @Query("UPDATE standalone_media SET type = :type WHERE id = :id")
    suspend fun updateMediaType(id: String, type: String)

    @Query("UPDATE standalone_media SET coverUrl = :coverUrl, localCoverPath = :localPath WHERE id = :id")
    suspend fun updateMediaCover(id: String, coverUrl: String?, localPath: String?)

    @Delete
    suspend fun deleteMedia(media: StandaloneMediaEntity)

    @Query("DELETE FROM standalone_media")
    suspend fun deleteAll()
}
