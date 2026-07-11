package com.example.phoenx.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineEntryDao {
    @Query("SELECT * FROM offline_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries ORDER BY createdAt DESC")
    suspend fun getAllEntriesSync(): List<OfflineEntry>

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

    @Query("SELECT * FROM amendments WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun getAmendmentsForEntrySync(entryId: String): List<AmendmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmendment(amendment: AmendmentEntity)

    @Query("SELECT aiSummary FROM offline_entries WHERE aiSummary != ''")
    suspend fun getAllAiSummaries(): List<String>

    // Portraits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortrait(portrait: PortraitEntity)

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    // Recipients
    @Query("SELECT * FROM recipients ORDER BY name ASC")
    fun getAllRecipients(): Flow<List<RecipientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipient(recipient: RecipientEntity)

    @Delete
    suspend fun deleteRecipient(recipient: RecipientEntity)

    // Depositaries
    @Query("SELECT * FROM depositaries LIMIT 1")
    fun getDepositary(): Flow<DepositaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepositary(depositary: DepositaryEntity)

    @Query("DELETE FROM depositaries")
    suspend fun clearDepositaries()

    // Legacies
    @Query("SELECT * FROM legacies ORDER BY createdAt DESC")
    fun getAllLegacies(): Flow<List<LegacyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegacy(legacy: LegacyEntity)

    @Delete
    suspend fun deleteLegacy(legacy: LegacyEntity)

    // Pacts
    @Query("SELECT * FROM pacts ORDER BY createdAt DESC")
    fun getAllPacts(): Flow<List<PactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPact(pact: PactEntity)

    @Query("SELECT * FROM offline_entries WHERE pactId = :pactId")
    fun getEntriesForPact(pactId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE locationId = :locationId")
    fun getEntriesForLocation(locationId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE compartmentIds LIKE '%,' || :compartmentId || ',%'")
    fun getEntriesByCompartment(compartmentId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE id = :entryId")
    fun getEntryById(entryId: String): Flow<OfflineEntry?>

    @Query("SELECT * FROM offline_entries WHERE recipientIds LIKE '%' || :recipientId || '%'")
    fun getEntriesForRecipient(recipientId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE recipientIds LIKE '%' || :recipientId || '%' AND entryType = 'PORTRAIT' LIMIT 1")
    fun getPortraitEntryForRecipient(recipientId: String): Flow<OfflineEntry?>

    @Query("SELECT * FROM offline_entries WHERE id IN (:ids)")
    fun getEntriesByIds(ids: List<String>): Flow<List<OfflineEntry>>

    @Query("UPDATE offline_entries SET aiSummary = :newSummary WHERE id = :entryId")
    suspend fun updateEntrySummary(newSummary: String, entryId: String): Int

    @Query("UPDATE offline_entries SET recipientIds = :newIds WHERE id = :entryId")
    suspend fun updateEntryRecipients(newIds: String, entryId: String): Int

    @Query("UPDATE offline_entries SET compartmentIds = :newCompartmentIds WHERE id = :entryId")
    suspend fun updateEntryCompartments(newCompartmentIds: String, entryId: String): Int

    @Query("UPDATE offline_entries SET emotionalCategory = :newCategory WHERE id = :entryId")
    suspend fun updateEntryCategory(newCategory: String, entryId: String): Int

    @Query("UPDATE offline_entries SET memoryDate = :newDate WHERE id = :entryId")
    suspend fun updateEntryMemoryDate(newDate: Long?, entryId: String): Int

    @Query("UPDATE offline_entries SET memoryDateStart = :start, memoryDateEnd = :end WHERE id = :entryId")
    suspend fun updateEntryMemoryPeriod(start: Long?, end: Long?, entryId: String): Int

    @Query("UPDATE offline_entries SET encryptedPayload = :newEncryptedPayload WHERE id = :entryId")
    suspend fun updateEntryContent(newEncryptedPayload: ByteArray, entryId: String): Int

    @Query("UPDATE offline_entries SET latitude = :lat, longitude = :lng, locationName = :name, locationId = :locId WHERE id = :entryId")
    suspend fun updateEntryLocation(lat: Double?, lng: Double?, name: String?, locId: String?, entryId: String): Int

    @Query("UPDATE offline_entries SET latitude = NULL, longitude = NULL, locationName = NULL, locationId = NULL WHERE id = :entryId")
    suspend fun detachEntryFromLocation(entryId: String): Int

    @Query("UPDATE offline_entries SET syncStatus = 'pending' WHERE syncStatus = 'synced'")
    suspend fun resetFalseSyncedEntries(): Int
}
