package com.example.phoenx.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: PersonMediaEntity)

    @Query("SELECT * FROM person_media WHERE personId = :personId ORDER BY capturedAt DESC")
    fun getMediaForPerson(personId: String): Flow<List<PersonMediaEntity>>

    @Delete
    suspend fun deleteMedia(media: PersonMediaEntity)

    @Query("SELECT * FROM person_media WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<PersonMediaEntity>

    @Query("UPDATE person_media SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
