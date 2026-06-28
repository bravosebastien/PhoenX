package com.example.phoenx.domain.manager

import com.example.phoenx.domain.models.SilenceConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

object SilenceManager {

    /**
     * Calcule le statut du silence en fonction de la config
     */
    fun checkSilenceStatus(config: SilenceConfig): SilenceStatus {
        if (config.missedCycles >= 3) return SilenceStatus.NOTIFY_DEPOSITARY
        if (config.missedCycles >= 2) return SilenceStatus.BLOCKED

        val lastDate = config.lastCheckInAt.toDate()
        val calendar = Calendar.getInstance()
        calendar.time = lastDate
        calendar.add(Calendar.DAY_OF_YEAR, config.rhythmDays)
        
        val now = Calendar.getInstance()
        
        return if (now.after(calendar)) {
            SilenceStatus.CHECK_IN_DUE
        } else {
            SilenceStatus.OK
        }
    }

    /**
     * Enregistre une présence du Créateur
     */
    suspend fun recordCheckIn(userId: String, status: String, db: FirebaseFirestore) {
        val updates = mapOf(
            "silenceConfig.lastCheckInAt" to Timestamp.now(),
            "silenceConfig.missedCycles" to 0,
            "silenceConfig.lastSilenceStatus" to status
        )
        db.collection("users").document(userId).update(updates).await()
    }

    /**
     * Incrémente les cycles manqués (Appelé généralement par Cloud Function)
     */
    suspend fun incrementMissedCycle(userId: String, db: FirebaseFirestore) {
        val doc = db.collection("users").document(userId).get().await()
        val currentMissed = doc.getLong("silenceConfig.missedCycles") ?: 0
        db.collection("users").document(userId).update("silenceConfig.missedCycles", currentMissed + 1).await()
    }
}

enum class SilenceStatus {
    OK, CHECK_IN_DUE, BLOCKED, NOTIFY_DEPOSITARY
}
