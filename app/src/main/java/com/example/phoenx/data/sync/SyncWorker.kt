package com.example.phoenx.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineEntryDao: OfflineEntryDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Récupération de l'utilisateur actuel
        val userId = FirebaseAuth.getInstance().currentUser?.uid 
            ?: return Result.failure() // Échec si déconnecté

        // Récupération des entrées en attente
        val pendingEntries = offlineEntryDao.getPendingEntries().first()
        
        if (pendingEntries.isEmpty()) return Result.success()

        val db = FirebaseFirestore.getInstance()
        var hasError = false

        return try {
            // Upload réel vers Firestore
            pendingEntries.forEach { entry ->
                try {
                    val firestoreMap = entry.toFirestoreMap()
                    
                    // Envoi vers le sous-collection "entries" de l'utilisateur
                    db.collection("users").document(userId)
                        .collection("entries").document(entry.id)
                        .set(firestoreMap)
                        .await()

                    // Confirmation de synchronisation locale
                    offlineEntryDao.updateSyncStatus(entry.id, "synced")
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "Erreur upload pour l'entrée ${entry.id}: ${e.message}")
                    hasError = true
                    // On continue avec les suivantes malgré l'échec d'une entrée
                }
            }
            
            if (hasError) Result.retry() else Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Erreur critique lors de la synchronisation: ${e.message}")
            Result.retry()
        }
    }
}
