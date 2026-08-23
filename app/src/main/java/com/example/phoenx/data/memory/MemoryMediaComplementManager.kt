package com.example.phoenx.data.memory

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.domain.model.CompartmentIds
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.sync.SyncTrigger
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryMediaComplementManager @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val auth: FirebaseAuth,
    private val syncTrigger: SyncTrigger,
    @ApplicationContext private val context: Context
) {

    suspend fun addMediaComplement(parentId: String, file: File, type: String, transcription: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val mediaDir = File(context.filesDir, "media")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            val destFile = File(mediaDir, "PHX_COMP_${UUID.randomUUID()}_${file.name}")
            file.inputStream().use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }

            var coverUrl: String? = null
            var localCoverPath: String? = null

            if (type == "VIDEO") {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(destFile.absolutePath)
                    val bitmap = retriever.getFrameAtTime(0)
                    retriever.release()

                    if (bitmap != null) {
                        val thumbFile = File(mediaDir, "THUMB_${destFile.name}.jpg")
                        FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                        }
                        // CORRECTIF v9.4.27 : On n'uploade PAS la miniature immédiatement (bloquant hors-ligne)
                        // On stocke uniquement son chemin local, le SyncWorker s'en chargera.
                        localCoverPath = thumbFile.absolutePath
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MemoryMediaComplementManager", "Échec extraction miniature vidéo", e)
                }
            }

            val parent = offlineEntryDao.getEntryById(parentId).first() ?: return

            // AUTOMATISME TIROIRS (v9.4.27) : Coche le tiroir correspondant au média ajouté
            val targetCompartment = when(type) {
                "PHOTO" -> CompartmentIds.PHOTOS
                "VIDEO" -> CompartmentIds.LIBRARY_VIDEO
                "AUDIO" -> CompartmentIds.LIBRARY_MUSIC
                else -> null
            }

            if (targetCompartment != null) {
                val currentIds = parent.compartmentIds.split(",").filter { it.isNotBlank() }.toMutableList()
                if (!currentIds.contains(targetCompartment)) {
                    currentIds.add(targetCompartment)
                    val csv = ",${currentIds.joinToString(",")},"
                    offlineEntryDao.updateEntryCompartments(csv, parentId)
                    android.util.Log.d("MemoryMediaComplementManager", "Automatisme : Tiroir $targetCompartment ajouté au parent $parentId")
                }
            }

            val finalTranscription = if (transcription.isNullOrBlank()) "Média complémentaire" else transcription

            val entry = OfflineEntry(
                id = UUID.randomUUID().toString(),
                creatorUid = parent.creatorUid,
                encryptedPayload = encryptionManager.encryptText(finalTranscription),
                entryType = type,
                ageAtCreation = parent.ageAtCreation,
                emotionalCategory = parent.emotionalCategory,
                visibility = parent.visibility,
                recipientIds = parent.recipientIds,
                parentEntryId = parentId,
                localMediaPath = destFile.absolutePath,
                coverUrl = coverUrl,
                localCoverPath = localCoverPath,
                aiSummary = finalTranscription,
                syncStatus = "pending"
            )
            offlineEntryDao.insertEntry(entry)
            syncTrigger.triggerSync(entry.id)
        } catch (e: Exception) {
            android.util.Log.e("MemoryMediaComplementManager", "Erreur ajout média", e)
            throw e // Renvoyé pour gestion d'erreur dans le ViewModel
        }
    }
}