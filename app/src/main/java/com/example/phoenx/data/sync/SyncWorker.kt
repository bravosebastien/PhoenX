package com.example.phoenx.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonMediaDao
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
    private val standaloneMediaDao: StandaloneMediaDao,
    private val personMediaDao: PersonMediaDao, // v9.4.22
    private val mediaManager: MediaManager,
    private val encryptionManager: EncryptionManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        /**
         * Déclenche une synchronisation immédiate (v9.4.24)
         */
        fun trigger(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val request = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "phoenx_immediate_sync",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        // Récupération de l'utilisateur actuel
        val userId = FirebaseAuth.getInstance().currentUser?.uid 
            ?: return Result.failure() // Échec si déconnecté

        // v9.4.24: Détection des données à synchroniser OU à nettoyer (Point 3)
        val pendingEntries = offlineEntryDao.getPendingEntries().first()
        
        val allPersons = offlineEntryDao.getAllPersons().first()
        val personsToSync = allPersons.filter { 
            it.syncStatus == "pending" || (!it.imagePath.isNullOrBlank() && it.imagePath!!.startsWith("/data/")) 
        }

        val pendingStandalone = standaloneMediaDao.getPendingSync()
        
        val allPersonMedia = personMediaDao.getAllMediaSync()
        val mediaToSync = allPersonMedia.filter {
            it.syncStatus == "pending" || (it.mediaPath.startsWith("/data/"))
        }

        // v9.4.27 : Profil Créateur (Ambiance Globale)
        val pendingProfile = offlineEntryDao.getPendingProfiles().firstOrNull()
        
        android.util.Log.d("PersonSync", "SyncWorker: ${pendingEntries.size} entrées, ${personsToSync.size} personnes, ${pendingStandalone.size} standalone, ${mediaToSync.size} personMedia et ${if (pendingProfile != null) 1 else 0} profil en attente")

        if (pendingEntries.isEmpty() && personsToSync.isEmpty() && pendingStandalone.isEmpty() && mediaToSync.isEmpty() && pendingProfile == null) return Result.success()

        val db = FirebaseFirestore.getInstance()
        var hasError = false
        val ensuredPersonIds = mutableSetOf<String>()

        return try {
            // 1. Synchronisation des Personnes (v8.8 + v8.9.9 Cameo Sync)
            personsToSync.forEach { person ->
                try {
                    android.util.Log.d("PersonSync", "Tentative upload pour : ${person.firstName} (${person.id})")
                    var storageUrl: String? = null
                    
                    // v9.4.24: Upload si chemin local (ne commence pas par "users/")
                    val path = person.imagePath
                    if (!path.isNullOrBlank() && (path.startsWith("/data/") || !path.startsWith("users/"))) {
                        val file = File(path)
                        if (file.exists()) {
                            android.util.Log.d("PersonSync", "Upload portrait Storage pour ${person.firstName}")
                            storageUrl = mediaManager.uploadCameo(userId, person.id, file)
                        }
                    } else {
                        storageUrl = path // C'est déjà une référence Storage
                    }

                    db.collection("users").document(userId)
                        .collection("persons").document(person.id)
                        .set(person.toFirestoreMap(storageUrl))
                        .await()
                    
                    ensuredPersonIds.add(person.id)
                    offlineEntryDao.insertPerson(person.copy(imagePath = storageUrl, syncStatus = "synced"))
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

                    // 1bis. GESTION DE L'UPLOAD MINIATURE (v9.4.27 : Fix Vidéo Offline)
                    var currentCoverUrl = entry.coverUrl
                    if (currentCoverUrl == null && !entry.localCoverPath.isNullOrEmpty()) {
                        val coverFile = File(entry.localCoverPath!!)
                        if (coverFile.exists()) {
                            android.util.Log.d("SyncWorker", "Début upload miniature pour ${entry.id}")
                            currentCoverUrl = mediaManager.encryptAndUpload(userId, "thumb_" + entry.id, coverFile)
                            
                            // Mémoriser en local
                            offlineEntryDao.updateEntryCover(currentCoverUrl, entry.localCoverPath, entry.id)
                            android.util.Log.d("SyncWorker", "Miniature uploadée avec succès : $currentCoverUrl")
                        }
                    }

                    // 2. PRÉPARATION DU MAP FIRESTORE (Incluant potentiellement la nouvelle URL)
                    // On recharge l'entrée depuis la DB si on a mis à jour l'URL
                    val entryToSync = if (currentMediaUrl != entry.mediaUrl || currentCoverUrl != entry.coverUrl) {
                        entry.copy(mediaUrl = currentMediaUrl, coverUrl = currentCoverUrl)
                    } else entry

                    val firestoreMap = entryToSync.toFirestoreMap(encryptionManager)
                    
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

            // 4. Synchronisation Person Media (Arbre Généalogique v9.4.22)
            mediaToSync.forEach { media ->
                try {
                    // Point 2 (v9.4.24): Garantir l'existence du parent
                    if (!ensuredPersonIds.contains(media.personId)) {
                        val parentDoc = db.collection("users").document(userId)
                            .collection("persons").document(media.personId).get().await()
                        
                        if (!parentDoc.exists()) {
                            // On tente de recréer le parent s'il manque sur Firestore mais existe en Room
                            val person = allPersons.find { it.id == media.personId }
                            if (person != null) {
                                db.collection("users").document(userId)
                                    .collection("persons").document(person.id)
                                    .set(person.toFirestoreMap(person.imagePath)) // imagePath local ou storage
                                    .await()
                            } else {
                                android.util.Log.e("SyncWorker", "Parent inexistant pour media ${media.id}")
                                return@forEach
                            }
                        }
                        ensuredPersonIds.add(media.personId)
                    }

                    var currentPath = media.mediaPath
                    
                    // Upload vers Storage si c'est un chemin local
                    if (currentPath.startsWith("/data/") || (!currentPath.startsWith("http") && !currentPath.startsWith("users/"))) {
                        val localFile = File(currentPath)
                        if (localFile.exists()) {
                            // On réutilise uploadCameo pour la simplicité (dossier persons)
                            currentPath = mediaManager.uploadCameo(userId, "person_media_${media.id}", localFile)
                            // Mise à jour locale pour éviter de re-uploader
                            personMediaDao.insertMedia(media.copy(mediaPath = currentPath))
                        }
                    }

                    val firestoreMap = media.toFirestoreMap()
                    db.collection("users").document(userId)
                        .collection("persons").document(media.personId)
                        .collection("media").document(media.id)
                        .set(firestoreMap)
                        .await()
                    
                    personMediaDao.updateSyncStatus(media.id, "synced")
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "Erreur upload personMedia ${media.id}: ${e.message}")
                    hasError = true
                }
            }

            // 5. Synchronisation du Profil Créateur (v9.4.27 : Ambiance Globale)
            if (pendingProfile != null) {
                try {
                    db.collection("users").document(userId)
                        .update(
                            "richProfile", pendingProfile.toFirestoreMap(),
                            "transmissionBackgroundId", pendingProfile.transmissionBackgroundId,
                            "transmissionFontId", pendingProfile.transmissionFontId
                        ).await()
                    
                    offlineEntryDao.insertCreatorProfile(pendingProfile.copy(syncStatus = "synced"))
                    android.util.Log.d("PersonSync", "Profil Créateur synchronisé avec succès")
                } catch (e: Exception) {
                    android.util.Log.e("PersonSync", "ÉCHEC synchronisation profil : ${e.message}")
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
