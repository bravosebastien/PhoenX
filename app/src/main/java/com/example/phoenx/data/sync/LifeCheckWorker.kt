package com.example.phoenx.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LifeCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val protocolManager: ActivationProtocolManager,
    private val auth: FirebaseAuth
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.success()
        
        return try {
            if (protocolManager.checkInactivity(userId)) {
                protocolManager.triggerAlert(userId)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
