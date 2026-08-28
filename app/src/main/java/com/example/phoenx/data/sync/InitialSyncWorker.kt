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
    private val mediaManager: MediaManager,
    private val encryptionManager: EncryptionManager,
    private val db: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        return try {
            // ═══ 1. RÉCUPÉRATION DES SOUVENIRS (ENTRIES) ═══
            val localEntries = offlineEntryDao.getAllEntriesSync()
            val localIds = localEntries.map { it.id }.toSet()

            val entriesSnapshot = db.collection("users").document(userId)
                .collection("entries")
                .get()
                .await()

            val remoteEntries = entriesSnapshot.documents.mapNotNull { it.toOfflineEntry(encryptionManager) }
            val missingEntries = remoteEntries.filter { it.id !in localIds }
            
            missingEntries.forEach { entry ->
                offlineEntryDao.insertEntry(entry.copy(syncStatus = "synced"))
            }

            // ═══ 2. RÉCUPÉRATION DES PERSONNES (CAMEOS) ═══
            val localPersons = offlineEntryDao.getAllPersons().first()
            val localPersonIds = localPersons.map { it.id }.toSet()

            val personsSnapshot = db.collection("users").document(userId)
                .collection("persons")
                .get()
                .await()

            val remotePersons = personsSnapshot.documents
            val missingPersonDocs = remotePersons.filter { it.id !in localPersonIds }

            missingPersonDocs.forEach { doc ->
                val person = doc.toPersonEntity()
                
                // v9.6.0 : Suppression du reclassement automatique FAMILY suspect.
                // On fait confiance aux données de Firestore ou à la valeur par défaut du mapper.
                val finalCategories = person.categories

                val storageUrl = doc.getString("imageUrl")
                var finalLocalPath: String? = null

                // Si la personne a un portrait sur Storage, on le télécharge
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

                offlineEntryDao.upsertPerson(person.copy(imagePath = finalLocalPath, categories = finalCategories))
            }

            // ═══ 3. RÉCUPÉRATION DES DESTINATAIRES (RECIPIENTS) — v9.4.19 ═══
            val recipientsSnapshot = db.collection("users").document(userId)
                .collection("recipients")
                .get()
                .await()

            recipientsSnapshot.documents.forEach { doc ->
                offlineEntryDao.insertRecipient(doc.toRecipientEntity())
            }

            // ═══ 4. RÉCUPÉRATION DES MÉDIAS DE L'ARBRE (v9.4.22) ═══
            // On itère sur les personnes déjà téléchargées à l'étape 2
            val persons = offlineEntryDao.getAllPersons().first()
            persons.forEach { person ->
                val mediaSnapshot = db.collection("users").document(userId)
                    .collection("persons").document(person.id)
                    .collection("media")
                    .get()
                    .await()
                
                mediaSnapshot.documents.forEach { doc ->
                    personMediaDao.insertMedia(doc.toPersonMediaEntity())
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

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
