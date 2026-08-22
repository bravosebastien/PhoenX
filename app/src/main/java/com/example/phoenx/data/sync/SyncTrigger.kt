package com.example.phoenx.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilitaire partagé pour déclencher une synchronisation WorkManager.
 * Extrait de MemoryDetailViewModel — étape 1/7 du découpage.
 */
@Singleton
class SyncTrigger @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    @ApplicationContext private val context: Context
) {
    suspend fun triggerSync(entryId: String) {
        offlineEntryDao.updateSyncStatus(entryId, "pending")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}