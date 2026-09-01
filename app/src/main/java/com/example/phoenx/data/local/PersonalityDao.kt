package com.example.phoenx.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalityDao {
    @Query("SELECT * FROM personalities ORDER BY name ASC")
    fun getAllPersonalities(): Flow<List<PersonalityEntity>>

    @Query("SELECT COUNT(*) FROM personalities")
    suspend fun getPersonalityCount(): Int

    @Query("SELECT * FROM personalities WHERE id = :id")
    fun getPersonalityById(id: String): Flow<PersonalityEntity?>

    @Query("SELECT * FROM personalities WHERE id = :id")
    suspend fun getPersonalityByIdSync(id: String): PersonalityEntity?

    @Upsert // v9.7.3 : Remplace @Insert(REPLACE) pour éviter DELETE CASCADE
    suspend fun insertPersonality(personality: PersonalityEntity)

    @Delete
    suspend fun deletePersonality(personality: PersonalityEntity)

    @Query("UPDATE personalities SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    // Media
    @Query("SELECT * FROM personality_media WHERE personalityId = :personalityId ORDER BY capturedAt ASC")
    fun getMediaForPersonality(personalityId: String): Flow<List<PersonalityMediaEntity>>

    @Upsert // v9.7.3 : Plus robuste
    suspend fun insertMedia(media: PersonalityMediaEntity)

    @Delete
    suspend fun deleteMedia(media: PersonalityMediaEntity)

    @Query("UPDATE personality_media SET syncStatus = :status WHERE id = :id")
    suspend fun updateMediaSyncStatus(id: String, status: String)

    @Query("SELECT * FROM personalities WHERE syncStatus = 'pending'")
    suspend fun getPendingPersonalities(): List<PersonalityEntity>

    @Query("SELECT * FROM personality_media WHERE syncStatus = 'pending'")
    suspend fun getPendingMedia(): List<PersonalityMediaEntity>
}
