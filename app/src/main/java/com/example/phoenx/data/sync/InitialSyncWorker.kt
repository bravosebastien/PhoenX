package com.example.phoenx.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonMediaDao
import com.example.phoenx.data.local.PersonalityEntity
import com.example.phoenx.data.local.PersonalityMediaEntity
import com.example.phoenx.data.local.StandaloneMediaDao
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.sync.toOfflineEntry
import com.example.phoenx.data.sync.toPersonEntity
import com.example.phoenx.data.sync.toStandaloneMediaEntity // v9.4.27
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * InitialSyncWorker (v8.9.9) : Synchronisation descendante (Firestore -> Room)
 * Se déclenche à la connexion pour restaurer la mémoire locale du Créateur.
 */
@HiltWorker
class InitialSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: StandaloneMediaDao,
    private val personMediaDao: PersonMediaDao, // v9.4.22
    private val personalityDao: com.example.phoenx.data.local.PersonalityDao, // v9.7.0
    private val mediaManager: MediaManager,
    private val encryptionManager: EncryptionManager,
    private val db: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        return try {
            // ═══ 1. RÉCUPÉRATION DES SOUVENIRS (ENTRIES + Reconciliation v9.6.7) ═══
            val localReferenceEntries = offlineEntryDao.getSyncedAndPendingDeletionEntriesSync()
            val localIds = localReferenceEntries.map { it.id }.toSet()

            val entriesSnapshot = db.collection("users").document(userId)
                .collection("entries")
                .get()
                .await()

            if (entriesSnapshot != null) {
                val remoteEntries = entriesSnapshot.documents.mapNotNull { it.toOfflineEntry(encryptionManager) }
                val remoteIds = remoteEntries.map { it.id }.toSet()

                android.util.Log.d("EntrySyncDebug", "Snapshot reçu : ${remoteEntries.size} documents distants.")
                
                // --- A. MISE À JOUR ET RESTAURATION ---
                remoteEntries.forEach { remoteEntry ->
                    // Upsert systématique pour synchroniser les changements
                    // L'objet remoteEntry a markedForDeletionAt = null, ce qui restaure les entrées si elles reviennent
                    offlineEntryDao.insertEntry(remoteEntry.copy(syncStatus = "synced"))
                    
                    val local = localReferenceEntries.find { it.id == remoteEntry.id }
                    if (local?.markedForDeletionAt != null) {
                        android.util.Log.d("EntrySyncDebug", "Restauration : ${remoteEntry.id} (réapparu sur le serveur)")
                    }
                }

                // --- B. RÉCONCILIATION SÉCURISÉE (SOFT-DELETE) ---
                val localReferenceCount = localReferenceEntries.size
                val remoteCount = remoteEntries.size

                // Seuil de sécurité : au moins 50% de présence ou min 1 si local non vide (anti-vidage massif)
                val isThresholdSafe = remoteCount >= (localReferenceCount / 2) && (remoteCount > 0 || localReferenceCount == 0)

                if (isThresholdSafe) {
                    localReferenceEntries.forEach { local ->
                        if (local.id !in remoteIds) {
                            if (local.markedForDeletionAt == null) {
                                // Cas A : 1ère absence -> Marquage
                                android.util.Log.d("EntrySyncDebug", "Marquage pour suppression (Étape 1) : ${local.id}")
                                offlineEntryDao.markForDeletion(local.id, System.currentTimeMillis())
                            } else {
                                // Cas B : 2ème absence -> Suppression physique
                                android.util.Log.d("EntrySyncDebug", "Suppression CONFIRMÉE (Étape 2) : ${local.id}")
                                
                                // Action atomique de nettoyage des dépendances
                                offlineEntryDao.deleteAmendmentsByEntryId(local.id)
                                offlineEntryDao.deleteComplementsByParentId(local.id)
                                offlineEntryDao.deleteEntry(local.id)
                            }
                        }
                    }
                } else {
                    android.util.Log.w("EntrySyncDebug", "ALERTE ANOMALIE : remoteCount ($remoteCount) suspect par rapport au local ($localReferenceCount). Suppression annulée.")
                }
            }

            // ═══ 2. RÉCUPÉRATION DES PERSONNES (CAMEOS + Reconciliation v9.6.7) ═══
            val localPersons = offlineEntryDao.getAllPersonsSync() // v9.6.7: Utilisation sync
            val syncedLocalPersonIds = localPersons.filter { it.syncStatus == "synced" }.map { it.id }.toSet()

            val personsSnapshot = db.collection("users").document(userId)
                .collection("persons")
                .get()
                .await()

            if (personsSnapshot != null) {
                val remotePersonDocs = personsSnapshot.documents
                val remoteIds = remotePersonDocs.map { it.id }.toSet()

                // 1. Mise à jour ou ajout des personnes distantes
                remotePersonDocs.forEach { doc ->
                    val person = doc.toPersonEntity()
                    val finalCategories = person.categories
                    val storageUrl = doc.getString("imageUrl")
                    var finalLocalPath: String? = null

                    // 1. Portrait Arbre (Public/Cameo)
                    if (!storageUrl.isNullOrBlank()) {
                        try {
                            val cameoDir = File(appContext.filesDir, "cameos")
                            if (!cameoDir.exists()) cameoDir.mkdirs()
                            val destFile = File(cameoDir, "cameo_${person.id}.jpg")
                            mediaManager.downloadCameo(storageUrl, destFile)
                            finalLocalPath = destFile.absolutePath
                        } catch (e: Exception) {
                            android.util.Log.e("InitialSyncWorker", "Erreur download portrait pour ${person.id}")
                        }
                    }

                    // 2. Portrait Rencontre (Chiffré - v9.7.9)
                    val encounterUrl = doc.getString("encounterImagePath")
                    var finalEncounterLocalPath: String? = person.encounterImagePath
                    
                    if (!encounterUrl.isNullOrBlank() && !encounterUrl.startsWith("/")) {
                        try {
                            val encounterDir = File(appContext.filesDir, "encounters")
                            if (!encounterDir.exists()) encounterDir.mkdirs()
                            val destFile = File(encounterDir, "encounter_${person.id}.jpg")
                            
                            // Téléchargement et déchiffrement immédiat pour mise en cache disque
                            val decryptedBytes = mediaManager.downloadAndDecrypt(encounterUrl)
                            destFile.writeBytes(decryptedBytes)
                            finalEncounterLocalPath = destFile.absolutePath
                            android.util.Log.d("InitialSyncWorker", "Portrait Rencontre pré-chargé et déchiffré: ${person.firstName}")
                        } catch (e: Exception) {
                            android.util.Log.e("InitialSyncWorker", "Échec pré-chargement portrait rencontre: ${person.id}")
                        }
                    }

                    // Upsert systématique avec les deux chemins locaux
                    offlineEntryDao.upsertPerson(person.copy(
                        imagePath = finalLocalPath, 
                        encounterImagePath = finalEncounterLocalPath,
                        categories = finalCategories,
                        syncStatus = "synced"
                    ))
                }

                // 2. RÉCONCILIATION : Suppression des personnes fantômes (supprimées ailleurs)
                // Room gérera automatiquement la suppression en cascade des médias liés (PersonMediaEntity)
                syncedLocalPersonIds.forEach { localId ->
                    if (localId !in remoteIds) {
                        android.util.Log.d("SyncReconciliation", "Suppression personne fantôme locale : id=$localId")
                        val personToDelete = localPersons.find { it.id == localId }
                        if (personToDelete != null) {
                            offlineEntryDao.deletePerson(personToDelete)
                        }
                    }
                }
            }

            // ═══ 3. RÉCUPÉRATION DES DESTINATAIRES (RECIPIENTS) — v9.4.19 ═══
            val recipientsSnapshot = db.collection("users").document(userId)
                .collection("recipients")
                .get()
                .await()

            recipientsSnapshot.documents.forEach { doc ->
                offlineEntryDao.insertRecipient(doc.toRecipientEntity())
            }

            // ═══ 4. RÉCUPÉRATION DES MÉDIAS DE L'ARBRE (v9.4.22 + Reconciliation v9.6.7) ═══
            // On itère sur les personnes déjà téléchargées à l'étape 2
            val persons = offlineEntryDao.getAllPersonsSync()
            persons.forEach { person ->
                try {
                    val mediaSnapshot = db.collection("users").document(userId)
                        .collection("persons").document(person.id)
                        .collection("media")
                        .get()
                        .await()
                    
                    if (mediaSnapshot != null) {
                        val remoteMediaEntities = mediaSnapshot.documents.map { it.toPersonMediaEntity() }
                        val remoteIds = remoteMediaEntities.map { it.id }.toSet()

                        // 1. Ajout/Mise à jour des médias distants
                        remoteMediaEntities.forEach { entity ->
                            var finalPath = entity.mediaPath
                            var finalThumbPath = entity.thumbnailPath
                            
                            // Si c'est un média de rencontre chiffré, on le pré-télécharge (v9.7.9)
                            if (person.categories.contains(",ENCOUNTER,") && !finalPath.startsWith("/")) {
                                try {
                                    val encounterMediaDir = File(appContext.filesDir, "encounter_media")
                                    if (!encounterMediaDir.exists()) encounterMediaDir.mkdirs()
                                    
                                    val destFile = File(encounterMediaDir, "${entity.id}${if(entity.mediaType == "VIDEO") ".mp4" else ".jpg"}")
                                    if (!destFile.exists()) {
                                        val decryptedBytes = mediaManager.downloadAndDecrypt(finalPath)
                                        destFile.writeBytes(decryptedBytes)
                                    }
                                    finalPath = destFile.absolutePath
                                    
                                    // Gestion miniature vidéo chiffrée
                                    if (entity.mediaType == "VIDEO" && !finalThumbPath.isNullOrBlank() && !finalThumbPath.startsWith("/")) {
                                        val thumbFile = File(encounterMediaDir, "thumb_${entity.id}.jpg")
                                        if (!thumbFile.exists()) {
                                            val thumbBytes = mediaManager.downloadAndDecrypt(finalThumbPath)
                                            thumbFile.writeBytes(thumbBytes)
                                        }
                                        finalThumbPath = thumbFile.absolutePath
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("InitialSyncWorker", "Échec pré-chargement média rencontre: ${entity.id}")
                                }
                            }
                            
                            personMediaDao.insertMedia(entity.copy(
                                mediaPath = finalPath,
                                thumbnailPath = finalThumbPath,
                                syncStatus = "synced"
                            ))
                        }

                        // 2. RÉCONCILIATION : Suppression des entrées locales qui n'existent plus sur le serveur
                        // On ne traite que les médias dont le statut est "synced" pour éviter d'effacer un média en cours d'upload.
                        val localMedia = personMediaDao.getMediaForPerson(person.id).first()
                        val syncedLocalMedia = localMedia.filter { it.syncStatus == "synced" }
                        
                        syncedLocalMedia.forEach { local ->
                            if (local.id !in remoteIds) {
                                android.util.Log.d("SyncReconciliation", "Suppression média fantôme local : id=${local.id} (Personne=${person.id})")
                                personMediaDao.deleteMedia(local)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SyncReconciliation", "Erreur lors de la réconciliation media pour ${person.id} : ${e.message}")
                    // SÉCURITÉ : En cas d'erreur de lecture Firestore, on ne supprime RIEN localement.
                }
            }

            // ═══ 5. RÉCUPÉRATION DES MÉDIAS STANDALONE (v9.4.27) ═══
            val standaloneSnapshot = db.collection("users").document(userId)
                .collection("standaloneMedia")
                .get()
                .await()
            
            standaloneSnapshot.documents.forEach { doc ->
                standaloneMediaDao.insertMedia(doc.toStandaloneMediaEntity())
            }

            // ═══ 6. RÉCUPÉRATION DES PERSONNALITÉS (v9.7.0 + Mirroring) ═══
            android.util.Log.d("PHOENX_SYNC_PERSO", "InitialSyncWorker: Démarrage sync personnalités")
            try {
                val localPersonalities = personalityDao.getAllPersonalities().first()
                val syncedLocalPersoIds = localPersonalities.filter { it.syncStatus == "synced" }.map { it.id }.toSet()

                val personalitiesSnapshot = db.collection("users").document(userId)
                    .collection("personalities")
                    .get()
                    .await()

                if (personalitiesSnapshot != null) {
                    val remotePersoDocs = personalitiesSnapshot.documents
                    val remotePersoIds = remotePersoDocs.map { it.id }.toSet()
                    android.util.Log.d("PHOENX_SYNC_PERSO", "InitialSyncWorker: ${remotePersoDocs.size} personnalités trouvées sur Firestore")

                    remotePersoDocs.forEach { doc ->
                        val mainPhotoUrl = doc.getString("mainPhotoPath")
                        var finalLocalPath: String? = null
                        
                        if (!mainPhotoUrl.isNullOrBlank()) {
                            try {
                                val persoDir = File(appContext.filesDir, "personalities")
                                if (!persoDir.exists()) persoDir.mkdirs()
                                val destFile = File(persoDir, "main_${doc.id}.jpg")
                                mediaManager.downloadCameo(mainPhotoUrl, destFile)
                                finalLocalPath = destFile.absolutePath
                            } catch (e: Exception) {
                                android.util.Log.e("PHOENX_SYNC_PERSO", "InitialSyncWorker: Erreur download main photo pour perso ${doc.id}", e)
                            }
                        }

                        val entity = PersonalityEntity(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            category = doc.getString("category") ?: "Autre",
                            customCategoryLabel = doc.getString("customCategoryLabel"),
                            mainPhotoPath = finalLocalPath ?: mainPhotoUrl ?: "",
                            biography = doc.getString("biography") ?: "",
                            personalComment = doc.getString("personalComment") ?: "",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            syncStatus = "synced"
                        )
                        personalityDao.insertPersonality(entity)
                        android.util.Log.d("PHOENX_SYNC_PERSO", "InitialSyncWorker: Personnalité insérée/mise à jour dans Room: ${entity.name}")

                        // Synchronisation de la galerie media
                        val mediaSnapshot = db.collection("users").document(userId)
                            .collection("personalities").document(doc.id)
                            .collection("media")
                            .get()
                            .await()
                        
                        if (mediaSnapshot != null) {
                            val remoteMediaIds = mediaSnapshot.documents.map { it.id }.toSet()
                            mediaSnapshot.documents.forEach { mediaDoc ->
                                val mediaEntity = PersonalityMediaEntity(
                                    id = mediaDoc.id,
                                    personalityId = doc.id,
                                    mediaPath = mediaDoc.getString("mediaPath") ?: "",
                                    capturedAt = mediaDoc.getLong("capturedAt") ?: System.currentTimeMillis(),
                                    syncStatus = "synced"
                                )
                                personalityDao.insertMedia(mediaEntity)
                            }

                            // Réconciliation galerie
                            val localMedia = personalityDao.getMediaForPersonality(doc.id).first()
                            localMedia.filter { it.syncStatus == "synced" }.forEach { local ->
                                if (local.id !in remoteMediaIds) {
                                    personalityDao.deleteMedia(local)
                                }
                            }
                        }
                    }

                    // Réconciliation personnalités
                    syncedLocalPersoIds.forEach { localId ->
                        if (localId !in remotePersoIds) {
                            localPersonalities.find { it.id == localId }?.let {
                                personalityDao.deletePersonality(it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_SYNC_PERSO", "InitialSyncWorker: ÉCHEC bloc personnalités", e)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_SYNC_PERSO", "InitialSyncWorker: ERREUR CRITIQUE", e)
            Result.retry()
        }
    }
}
