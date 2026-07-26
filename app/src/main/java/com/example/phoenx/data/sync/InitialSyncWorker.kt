package com.example.phoenx.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.sync.toOfflineEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * InitialSyncWorker (v8.9.9) : Synchronisation descendante (Firestore -> Room)
 * Se déclenche à la connexion pour restaurer la mémoire locale du Créateur.
 */
@HiltWorker
class InitialSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineEntryDao: OfflineEntryDao,
    private val db: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        return try {
            // 1. Récupération des IDs déjà présents en local pour éviter d'écraser des modifications non sync
            val localEntries = offlineEntryDao.getAllEntriesSync()
            val localIds = localEntries.map { it.id }.toSet()

            // 2. Récupération de tous les souvenirs du Créateur sur Firestore
            val entriesSnapshot = db.collection("users").document(userId)
                .collection("entries")
                .get()
                .await()

            if (entriesSnapshot.isEmpty) {
                return Result.success()
            }

            val remoteEntries = entriesSnapshot.documents.mapNotNull { it.toOfflineEntry() }
            
            // 3. Calcul du différentiel : on ne garde que ce qui n'est PAS en local
            val missingEntries = remoteEntries.filter { it.id !in localIds }
            
            if (missingEntries.isNotEmpty()) {
                missingEntries.forEach { entry ->
                    // On insère avec le statut 'synced'
                    offlineEntryDao.insertEntry(entry.copy(syncStatus = "synced"))
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
