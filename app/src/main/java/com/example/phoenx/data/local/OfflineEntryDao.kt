package com.example.phoenx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineEntryDao {
    @Query("SELECT * FROM offline_entries WHERE syncStatus = 'pending'")
    fun getPendingEntries(): Flow<List<OfflineEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: OfflineEntry)

    @Query("UPDATE offline_entries SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM offline_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)
}
