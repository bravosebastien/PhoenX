package com.example.phoenx.data.memory

import android.content.Context
import android.net.Uri
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.*
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.sync.SyncTrigger
import com.example.phoenx.domain.util.EnigmaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryMetadataUpdater @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val syncTrigger: SyncTrigger,
    private val encryptionManager: EncryptionManager,
    private val mediaManager: MediaManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {

    suspend fun updateContent(entryId: String, newText: String) {
        try {
            val encrypted = encryptionManager.encryptText(newText)
            offlineEntryDao.updateEntryContent(encrypted, entryId)
            syncTrigger.triggerSync(entryId)
        } catch (e: Exception) {
            android.util.Log.e("MemoryMetadataUpdater", "Error updating content", e)
        }
    }

    suspend fun updateTitle(entryId: String, newTitle: String) {
        try {
            offlineEntryDao.updateEntrySummary(newTitle, entryId)
            syncTrigger.triggerSync(entryId)
        } catch (e: Exception) {
            android.util.Log.e("MemoryMetadataUpdater", "Error updating title", e)
        }
    }

    suspend fun updateComplementTitle(complementId: String, newTitle: String) {
        offlineEntryDao.updateEntryMediaTitle(newTitle, complementId)
        syncTrigger.triggerSync(complementId)
    }

    suspend fun updateComplementComment(complementId: String, newComment: String?) {
        offlineEntryDao.updateEntryComment(newComment, complementId)
        syncTrigger.triggerSync(complementId)
    }

    suspend fun updateComplementCover(complementId: String, imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val file = uriToFile(imageUri) ?: return
        try {
            val storagePath = mediaManager.encryptAndUpload(uid, complementId, file)
            offlineEntryDao.updateEntryCover(storagePath, file.absolutePath, complementId)
            syncTrigger.triggerSync(complementId)
        } catch (e: Exception) {
            android.util.Log.e("MemoryMetadataUpdater", "Erreur upload couverture", e)
        }
    }

    suspend fun updateIncludedInBook(entryId: String, included: Boolean) {
        offlineEntryDao.updateIncludedInBook(entryId, included)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateRecipients(entryId: String, newRecipientDocIds: List<String>, allRecipients: List<RecipientEntity>) {
        val persistentIds = newRecipientDocIds.map { docId ->
            allRecipients.find { it.id == docId }?.linkedUid ?: docId
        }.distinct()
        val idsCsv = persistentIds.joinToString(",")
        offlineEntryDao.updateEntryRecipients(idsCsv, entryId)
        // v9.4.27 Fix C : Cascade aux compléments
        offlineEntryDao.updateComplementsRecipients(idsCsv, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun toggleRecipient(entryId: String, currentSelectedDocIds: List<String>, docId: String, allRecipients: List<RecipientEntity>) {
        android.util.Log.d("PHOENX_CLICK_TRACE", "toggleRecipient (Updater) id=$entryId")
        val newList = if (currentSelectedDocIds.contains(docId)) {
            currentSelectedDocIds.filter { it != docId }
        } else {
            currentSelectedDocIds + docId
        }
        updateRecipients(entryId, newList, allRecipients)
    }

    suspend fun updateEnigma(
        entryId: String,
        question: String?,
        answer: String?,
        hint: String?,
        autoUnlockDays: Int?,
        isUltimate: Boolean
    ) {
        val currentEntry = offlineEntryDao.getEntryById(entryId).first() ?: return
        val newHash = if (!answer.isNullOrBlank()) {
            EnigmaUtils.hashAnswer(answer)
        } else {
            currentEntry.enigmaAnswer
        }

        offlineEntryDao.updateEntryEnigma(
            question = question,
            answerHash = newHash,
            hint = hint,
            unlockDays = autoUnlockDays,
            isUltimate = isUltimate,
            entryId = entryId
        )
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateEntryVisibility(entryId: String, visibility: String) {
        offlineEntryDao.updateEntryVisibility(visibility, entryId)
        // v9.4.27 Fix C : Cascade aux compléments
        offlineEntryDao.updateComplementsVisibility(visibility, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateEntryRecipients(entryId: String, recipientDocIds: List<String>, allRecipients: List<RecipientEntity>) {
        val persistentIds = recipientDocIds.map { docId ->
            allRecipients.find { it.id == docId }?.linkedUid ?: docId
        }.distinct()
        offlineEntryDao.updateEntryRecipients(persistentIds.joinToString(","), entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateVisibility(entryId: String, visibility: String) {
        updateEntryVisibility(entryId, visibility)
    }

    suspend fun updateSilentAttribution(entryId: String, silent: Boolean) {
        offlineEntryDao.updateEntrySilentAttribution(silent, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateIncludeInBook(entryId: String, include: Boolean) {
        offlineEntryDao.updateEntryIncludeInBook(include, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updatePactId(entryId: String, pactId: String?) {
        offlineEntryDao.updateEntryPactId(pactId, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateCompartments(entryId: String, selectedIds: List<String>) {
        val csv = if (selectedIds.isEmpty()) "" else ",${selectedIds.joinToString(",")},"
        offlineEntryDao.updateEntryCompartments(csv, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateCategory(entryId: String, category: String) {
        android.util.Log.d("PHOENX_CLICK_TRACE", "updateCategory (Updater) id=$entryId")
        offlineEntryDao.updateEntryCategory(category, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateTonalNuance(entryId: String, nuance: String) {
        offlineEntryDao.updateEntryTonalNuance(nuance.take(150), entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateMemoryDate(entryId: String, date: Long?) {
        offlineEntryDao.updateEntryMemoryDate(date, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateMemoryPeriod(entryId: String, start: Long?, end: Long?) {
        offlineEntryDao.updateEntryMemoryPeriod(start, end, entryId)
        syncTrigger.triggerSync(entryId)
    }

    suspend fun updateLocation(entryId: String, lat: Double?, lng: Double?, name: String?, locId: String?) {
        android.util.Log.d("PHOENX_MAP_TRACE", "updateLocation (Updater): entryId=$entryId, locationId=$locId, lat=$lat, lng=$lng, name=$name")
        try {
            offlineEntryDao.updateEntryLocation(lat, lng, name, locId, entryId)
            android.util.Log.d("PHOENX_MAP_TRACE", "   -> Écriture Room réussie")
            syncTrigger.triggerSync(entryId)
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_MAP_TRACE", "   -> ÉCHEC écriture Room: ${e.message}")
        }
    }

    suspend fun assignLocationFromId(entryId: String, locationId: String) {
        android.util.Log.d("PHOENX_MAP_TRACE", "assignLocationFromId (Updater): entryId=$entryId, locationId=$locationId")
        val uid = auth.currentUser?.uid ?: return
        try {
            val doc = db.collection("users").document(uid)
                .collection("locations").document(locationId).get().await()
            
            if (doc.exists()) {
                val lat = doc.getDouble("latitude")
                val lng = doc.getDouble("longitude")
                val name = doc.getString("placeName")
                android.util.Log.d("PHOENX_MAP_TRACE", "   -> Données Firestore récupérées: lat=$lat, lng=$lng, name=$name")
                
                updateLocation(entryId, lat, lng, name, locationId)
            } else {
                android.util.Log.w("PHOENX_MAP_TRACE", "   -> Document Lieu Firestore INTROUVABLE: $locationId")
            }
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_MAP_TRACE", "   -> ERREUR résolution lieu Firestore: ${e.message}", e)
        }
    }

    fun uriToFile(uri: Uri): File? {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType?.contains("video") == true) {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "temp_media_${UUID.randomUUID()}.mp4")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                tempFile
            } catch (e: Exception) {
                null
            }
        } else {
            // v9.7.9 : Compression unifiée (Modèle Rencontres)
            return com.example.phoenx.ui.util.ImageUtils.compressAndResize(context, uri)
        }
    }
}
