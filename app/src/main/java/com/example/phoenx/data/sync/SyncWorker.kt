package com.example.phoenx.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.StandaloneMediaDao
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.sync.toFirestoreMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: StandaloneMediaDao, // v9.3.2
    private val mediaManager: MediaManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Récupération de l'utilisateur actuel
        val userId = FirebaseAuth.getInstance().currentUser?.uid 
            ?: return Result.failure() // Échec si déconnecté

        // Récupération des entrées en attente
        val pendingEntries = offlineEntryDao.getPendingEntries().first()
        val pendingPersons = offlineEntryDao.getAllPersons().first().filter { it.syncStatus == "pending" }
        val pendingStandalone = standaloneMediaDao.getPendingSync() // v9.3.2
        
        android.util.Log.d("PersonSync", "SyncWorker: ${pendingEntries.size} entrées, ${pendingPersons.size} personnes et ${pendingStandalone.size} standalone en attente")

        if (pendingEntries.isEmpty() && pendingPersons.isEmpty() && pendingStandalone.isEmpty()) return Result.success()

        val db = FirebaseFirestore.getInstance()
        var hasError = false

        return try {
            // 1. Synchronisation des Personnes (v8.8 + v8.9.9 Cameo Sync)
            pendingPersons.forEach { person ->
                try {
                    android.util.Log.d("PersonSync", "Tentative upload pour : ${person.firstName} (${person.id})")
                    var storageUrl: String? = null
                    
                    // Upload du portrait vers Storage si présent (v8.9.9)
                    if (!person.imagePath.isNullOrBlank()) {
                        val file = File(person.imagePath)
                        if (file.exists()) {
                            android.util.Log.d("PersonSync", "Upload portrait Storage pour ${person.firstName}")
                            storageUrl = mediaManager.uploadCameo(userId, person.id, file)
                        }
                    }

                    db.collection("users").document(userId)
                        .collection("persons").document(person.id)
                        .set(person.toFirestoreMap(storageUrl))
                        .await()
                    
                    offlineEntryDao.insertPerson(person.copy(syncStatus = "synced"))
                    android.util.Log.d("PersonSync", "Upload Firestore RÉUSSI pour ${person.firstName}")
                } catch (e: Exception) {
                    android.util.Log.e("PersonSync", "ÉCHEC upload pour ${person.firstName}: ${e.message}")
                    hasError = true
                }
            }

            // 2. Upload réel des entrées vers Firestore
            pendingEntries.forEach { entry ->
                try {
                    var currentMediaUrl = entry.mediaUrl

                    // 1. GESTION DE L'UPLOAD MÉDIA SI NÉCESSAIRE (Signature 7.3)
                    if (currentMediaUrl == null && !entry.localMediaPath.isNullOrEmpty()) {
                        val localFile = File(entry.localMediaPath)
                        if (localFile.exists()) {
                            android.util.Log.d("SyncWorker", "Début upload média pour ${entry.id}")
                            currentMediaUrl = mediaManager.encryptAndUpload(userId, entry.id, localFile)
                            
                            // Mémoriser l'URL en local pour ne pas re-uploader en cas d'échec Firestore
                            offlineEntryDao.updateEntryMediaUrl(currentMediaUrl, entry.id)
                            android.util.Log.d("SyncWorker", "Média uploadé avec succès : $currentMediaUrl")
                        }
                    }

                    // 2. PRÉPARATION DU MAP FIRESTORE (Incluant potentiellement la nouvelle URL)
                    // On recharge l'entrée depuis la DB si on a mis à jour l'URL
                    val entryToSync = if (currentMediaUrl != entry.mediaUrl) {
                        entry.copy(mediaUrl = currentMediaUrl)
                    } else entry

                    val firestoreMap = entryToSync.toFirestoreMap()
                    
                    // 3. ENVOI VERS FIRESTORE
                    db.collection("users").document(userId)
                        .collection("entries").document(entry.id)
                        .set(firestoreMap)
                        .await()

                    // Confirmation de synchronisation locale
                    offlineEntryDao.updateSyncStatus(entry.id, "synced")
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "Erreur upload pour l'entrée ${entry.id}: ${e.message}")
                    hasError = true
                }
            }

            // 3. Synchronisation Standalone Media (v9.3.2)
            pendingStandalone.forEach { media ->
                try {
                    val firestoreMap = media.toFirestoreMap()
                    db.collection("users").document(userId)
                        .collection("standaloneMedia").document(media.id)
                        .set(firestoreMap)
                        .await()
                    
                    standaloneMediaDao.updateSyncStatus(media.id, "synced")
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "Erreur upload standalone ${media.id}: ${e.message}")
                    hasError = true
                }
            }
            
            if (hasError) Result.retry() else Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Erreur critique lors de la synchronisation: ${e.message}")
            Result.retry()
        }
    }
}
