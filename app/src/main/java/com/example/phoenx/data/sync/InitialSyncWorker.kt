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

        android.util.Log.d("FIL_DEBUG", "InitialSyncWorker: Démarrage de la récupération Firestore pour $userId")

        return try {
            // 1. Récupération des souvenirs du Créateur sur Firestore
            val entriesSnapshot = db.collection("users").document(userId)
                .collection("entries")
                .get()
                .await()

            if (entriesSnapshot.isEmpty) {
                android.util.Log.d("FIL_DEBUG", "InitialSyncWorker: Aucune donnée trouvée sur Firestore pour cet utilisateur.")
                return Result.success()
            }

            val entries = entriesSnapshot.documents.mapNotNull { it.toOfflineEntry() }
            
            android.util.Log.d("FIL_DEBUG", "InitialSyncWorker: ${entries.size} souvenirs trouvés. Début de l'insertion locale...")

            // 2. Insertion dans Room (Idempotent grâce au REPLACE du DAO)
            entries.forEach { entry ->
                // On force le statut 'synced' pour éviter que le SyncWorker ne les ré-uploade vers Firestore
                offlineEntryDao.insertEntry(entry.copy(syncStatus = "synced"))
            }

            android.util.Log.d("FIL_DEBUG", "InitialSyncWorker: Restauration locale terminée avec succès.")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("FIL_DEBUG", "InitialSyncWorker: ÉCHEC CRITIQUE : ${e.message}")
            Result.retry()
        }
    }
}
