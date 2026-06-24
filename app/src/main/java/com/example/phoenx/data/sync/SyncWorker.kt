package com.example.phoenx.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineEntryDao: OfflineEntryDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingEntries = offlineEntryDao.getPendingEntries().first()
        
        if (pendingEntries.isEmpty()) return Result.success()

        return try {
            // Logique d'upload vers Firestore à implémenter ici
            // Pour chaque entrée chiffrée, on tente l'envoi
            pendingEntries.forEach { entry ->
                // Simulation d'envoi réussi
                offlineEntryDao.updateSyncStatus(entry.id, "synced")
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
