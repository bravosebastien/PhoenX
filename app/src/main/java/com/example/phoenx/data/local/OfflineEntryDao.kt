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

    // v9.6.7 : Reconciliation
    @Query("SELECT * FROM offline_entries WHERE syncStatus = 'synced' OR markedForDeletionAt IS NOT NULL")
    suspend fun getSyncedAndPendingDeletionEntriesSync(): List<OfflineEntry>

    @Query("DELETE FROM amendments WHERE entryId = :entryId")
    suspend fun deleteAmendmentsByEntryId(entryId: String)

    @Query("DELETE FROM offline_entries WHERE parentEntryId = :parentId")
    suspend fun deleteComplementsByParentId(parentId: String)

    @Query("UPDATE offline_entries SET markedForDeletionAt = :timestamp WHERE id = :entryId")
    suspend fun markForDeletion(entryId: String, timestamp: Long?)

    @Query("UPDATE offline_entries SET includedInBook = :included WHERE id = :entryId")
    suspend fun updateIncludedInBook(entryId: String, included: Boolean): Int

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

    @Query("DELETE FROM recipients WHERE id = :id")
    suspend fun deleteRecipientById(id: String)

    // Persons (v8.8)
    @Query("SELECT * FROM persons ORDER BY firstName ASC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE categories LIKE '%,FAMILY,%' ORDER BY firstName ASC")
    fun getFamilyPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE categories LIKE '%,ENCOUNTER,%' ORDER BY firstName ASC")
    fun getEncounterPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons ORDER BY firstName ASC")
    suspend fun getAllPersonsSync(): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE firstName LIKE :query || '%'")
    suspend fun searchPersonsByFirstName(query: String): List<PersonEntity>

    @Upsert
    suspend fun upsertPerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("SELECT * FROM persons WHERE id IN (:ids)")
    suspend fun getPersonsByIds(ids: List<String>): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE id = :id")
    fun getPersonByIdFlow(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE parentIds LIKE '%,' || :personId || ',%'")
    fun getChildrenOf(personId: String): Flow<List<PersonEntity>>

    // Witnesses
    @Query("SELECT * FROM witnesses ORDER BY name ASC")
    fun getAllWitnesses(): Flow<List<WitnessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWitness(witness: WitnessEntity)

    @Query("DELETE FROM witnesses WHERE id = :witnessId")
    suspend fun deleteWitness(witnessId: String)

    // Notification Contacts
    @Query("SELECT * FROM notification_contacts ORDER BY addedAt ASC")
    fun getAllNotificationContacts(): Flow<List<NotificationContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationContact(contact: NotificationContactEntity)

    @Query("DELETE FROM notification_contacts WHERE id = :contactId")
    suspend fun deleteNotificationContact(contactId: String)

    // Depositaries
    @Query("SELECT * FROM depositaries ORDER BY role ASC")
    fun getAllDepositaries(): Flow<List<DepositaryEntity>>

    @Query("SELECT * FROM depositaries WHERE id = :id")
    suspend fun getDepositaryById(id: String): DepositaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepositary(depositary: DepositaryEntity)

    @Query("DELETE FROM depositaries WHERE id = :id")
    suspend fun deleteDepositary(id: String)

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

    @Query("SELECT * FROM pacts WHERE id = :pactId")
    suspend fun getPactById(pactId: String): PactEntity?

    @Query("SELECT * FROM offline_entries WHERE pactId = :pactId")
    fun getEntriesForPact(pactId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE locationId = :locationId")
    fun getEntriesForLocation(locationId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE compartmentIds LIKE '%,' || :compartmentId || ',%'")
    fun getEntriesByCompartment(compartmentId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE id = :entryId")
    fun getEntryById(entryId: String): Flow<OfflineEntry?>

    @Query("SELECT * FROM offline_entries WHERE parentEntryId = :parentId ORDER BY createdAt ASC")
    fun getComplements(parentId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE ((recipientIds LIKE '%' || :recipientId || '%') OR visibility = 'EVERYONE') AND parentEntryId IS NULL ORDER BY createdAt DESC")
    fun getEntriesForRecipientUnified(recipientId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE recipientIds LIKE '%' || :recipientId || '%' AND parentEntryId IS NULL")
    fun getEntriesForRecipient(recipientId: String): Flow<List<OfflineEntry>>

    @Query("SELECT * FROM offline_entries WHERE recipientIds LIKE '%' || :recipientId || '%' AND entryType = 'PORTRAIT' LIMIT 1")
    fun getPortraitEntryForRecipient(recipientId: String): Flow<OfflineEntry?>

    @Query("SELECT * FROM offline_entries WHERE id IN (:ids)")
    fun getEntriesByIds(ids: List<String>): Flow<List<OfflineEntry>>

    // Migration v59 : titre utilisateur (l'Étincelle), distinct du résumé fourni à l'IA du Livre.
    @Query("UPDATE offline_entries SET userTitle = :newTitle WHERE id = :entryId")
    suspend fun updateEntryTitle(newTitle: String, entryId: String): Int

    @Query("UPDATE offline_entries SET aiSummary = :title WHERE id = :entryId")
    suspend fun updateEntryMediaTitle(title: String, entryId: String): Int

    @Query("UPDATE offline_entries SET userComment = :newComment WHERE id = :entryId")
    suspend fun updateEntryComment(newComment: String?, entryId: String): Int

    @Query("UPDATE offline_entries SET coverUrl = :coverUrl, localCoverPath = :localPath WHERE id = :entryId")
    suspend fun updateEntryCover(coverUrl: String?, localPath: String?, entryId: String): Int

    @Query("UPDATE offline_entries SET mediaProvider = :provider WHERE id = :entryId")
    suspend fun updateEntryProvider(provider: String?, entryId: String): Int

    @Query("UPDATE offline_entries SET visibility = :visibility, syncStatus = 'pending' WHERE id = :entryId")
    suspend fun updateEntryVisibility(visibility: String, entryId: String): Int

    @Query("UPDATE offline_entries SET visibility = :visibility, syncStatus = 'pending' WHERE parentEntryId = :parentId")
    suspend fun updateComplementsVisibility(visibility: String, parentId: String): Int

    @Query("UPDATE offline_entries SET recipientIds = :newIds, syncStatus = 'pending' WHERE id = :entryId")
    suspend fun updateEntryRecipients(newIds: String, entryId: String): Int

    @Query("UPDATE offline_entries SET recipientIds = :newIds, syncStatus = 'pending' WHERE parentEntryId = :parentId")
    suspend fun updateComplementsRecipients(newIds: String, parentId: String): Int

    @Query("UPDATE offline_entries SET compartmentIds = :newCompartmentIds WHERE id = :entryId")
    suspend fun updateEntryCompartments(newCompartmentIds: String, entryId: String): Int

    @Query("UPDATE offline_entries SET emotionalCategory = :newCategory WHERE id = :entryId")
    suspend fun updateEntryCategory(newCategory: String, entryId: String): Int

    @Query("UPDATE offline_entries SET tonalNuance = :newNuance WHERE id = :entryId")
    suspend fun updateEntryTonalNuance(newNuance: String?, entryId: String): Int

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

    @Query("UPDATE offline_entries SET latitude = NULL, longitude = NULL, locationName = NULL, locationId = NULL WHERE locationId = :locationId")
    suspend fun detachAllEntriesFromLocation(locationId: String): Int

    @Query("UPDATE offline_entries SET mediaUrl = :url WHERE id = :entryId")
    suspend fun updateEntryMediaUrl(url: String, entryId: String): Int

    @Query("UPDATE offline_entries SET localMediaPath = :path WHERE id = :entryId")
    suspend fun updateEntryLocalPath(path: String, entryId: String): Int

    @Query("UPDATE offline_entries SET syncStatus = 'pending' WHERE syncStatus = 'synced'")
    suspend fun resetFalseSyncedEntries(): Int

    @Query("UPDATE offline_entries SET creatorUid = :uid WHERE creatorUid = '' OR creatorUid IS NULL")
    suspend fun repairEmptyCreatorUids(uid: String): Int

    @Query("UPDATE offline_entries SET personIds = :newPersonIds WHERE id = :entryId")
    suspend fun updateEntryPersons(newPersonIds: String, entryId: String): Int

    @Query("UPDATE offline_entries SET enigmaQuestion = :question, enigmaAnswer = :answerHash, enigmaHint = :hint, enigmaAutoUnlockDays = :unlockDays, isUltimateSecret = :isUltimate WHERE id = :entryId")
    suspend fun updateEntryEnigma(question: String?, answerHash: String?, hint: String?, unlockDays: Int?, isUltimate: Boolean, entryId: String): Int

    @Query("UPDATE offline_entries SET silentAttribution = :silent WHERE id = :entryId")
    suspend fun updateEntrySilentAttribution(silent: Boolean, entryId: String): Int

    @Query("UPDATE offline_entries SET includeInBook = :include WHERE id = :entryId")
    suspend fun updateEntryIncludeInBook(include: Boolean, entryId: String): Int

    @Query("UPDATE offline_entries SET pactId = :pactId WHERE id = :entryId")
    suspend fun updateEntryPactId(pactId: String?, entryId: String): Int

    // Creator Profile (v9.1)
    @Query("SELECT * FROM creator_profile WHERE userId = :userId")
    fun getCreatorProfile(userId: String): Flow<CreatorProfileEntity?>

    @Query("SELECT * FROM creator_profile WHERE userId = :userId")
    suspend fun getCreatorProfileSync(userId: String): CreatorProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreatorProfile(profile: CreatorProfileEntity)

    @Query("SELECT * FROM creator_profile WHERE syncStatus = 'pending'")
    suspend fun getPendingProfiles(): List<CreatorProfileEntity>
}
