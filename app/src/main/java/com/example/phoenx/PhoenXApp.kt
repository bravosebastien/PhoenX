package com.example.phoenx

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.data.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PhoenXApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var offlineEntryDao: OfflineEntryDao

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            // ETAPE 2 : Migration de rattrapage (une seule fois)
            if (!preferenceManager.isSyncMigrationV1Done().first()) {
                offlineEntryDao.resetFalseSyncedEntries()
                preferenceManager.setSyncMigrationV1Done(true)
            }

            // ETAPE 4 : Programmer le Worker périodique
            schedulePeriodicSync()
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "phoenx_sync_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}
