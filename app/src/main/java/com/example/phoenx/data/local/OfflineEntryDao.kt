package com.example.phoenx.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineEntryDao {
    @Query("SELECT * FROM offline_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE syncStatus = 'pending'")
    fun getPendingEntries(): Flow<List<OfflineEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: OfflineEntry)

    @Query("UPDATE offline_entries SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM offline_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    // Amendments
    @Query("SELECT * FROM amendments WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun getAmendmentsForEntry(entryId: String): Flow<List<AmendmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmendment(amendment: AmendmentEntity)

    // Portraits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortrait(portrait: PortraitEntity)
}
