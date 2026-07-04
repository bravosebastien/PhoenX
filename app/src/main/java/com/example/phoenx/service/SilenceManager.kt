package com.example.phoenx.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SilenceManager @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // Vérifie le statut du silence au lancement
    suspend fun checkSilenceStatus(userId: String): SilenceStatus {
        val doc = db.collection("users")
            .document(userId).get().await()

        val config = doc.get("silenceConfig") as? Map<*, *>
            ?: return SilenceStatus.OK

        val rhythmDays = (config["rhythmDays"] as? Long)?.toInt() ?: 30
        val lastCheckIn = config["lastCheckInAt"] as? Timestamp
            ?: return SilenceStatus.CHECK_IN_DUE
        val missedCycles = (config["missedCycles"] as? Long)?.toInt() ?: 0

        val daysSinceCheckIn = (System.currentTimeMillis() -
            lastCheckIn.toDate().time) / (1000 * 60 * 60 * 24)

        return when {
            missedCycles >= 2 -> SilenceStatus.BLOCKED
            daysSinceCheckIn >= rhythmDays -> SilenceStatus.CHECK_IN_DUE
            else -> SilenceStatus.OK
        }
    }

    // Enregistre une confirmation de présence
    suspend fun recordCheckIn(
        userId: String,
        status: String // "present" | "traversing"
    ) {
        db.collection("users").document(userId).update(
            mapOf(
                "silenceConfig.lastCheckInAt" to Timestamp.now(),
                "silenceConfig.missedCycles" to 0,
                "silenceConfig.escalationLevel" to 0,
                "silenceConfig.lastSilenceStatus" to status
            )
        ).await()
    }

    // Enregistre un cycle manqué
    suspend fun incrementMissedCycle(userId: String) {
        val doc = db.collection("users")
            .document(userId).get().await()
        val current = (doc.getLong("silenceConfig.missedCycles") ?: 0L)
        db.collection("users").document(userId).update(
            "silenceConfig.missedCycles", current + 1
        ).await()
    }
}

enum class SilenceStatus {
    OK,             // Tout va bien, ouvrir l'app normalement
    CHECK_IN_DUE,   // Check-in requis, afficher SilenceCheckInScreen
    BLOCKED         // Bloqué, afficher SilenceBlockScreen
}
