package com.example.phoenx.domain.usecase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivationProtocolManager @Inject constructor(
    private val db: FirebaseFirestore,
    private val functions: com.google.firebase.functions.FirebaseFunctions
) {
    /**
     * Confirme que le Créateur est en vie (Action hebdomadaire).
     */
    suspend fun confirmProofOfLife(userId: String) {
        try {
            // Appeler la Cloud Function sécurisée (v9.4.14)
            functions.getHttpsCallable("confirmCreatorProofOfLife").call().await()
        } catch (e: Exception) {
            android.util.Log.e("ProtocolManager", "Erreur lors de la confirmation de vie", e)
            throw e
        }
    }

    /**
     * Vérifie si le délai d'inactivité (ex: 21 jours) est dépassé.
     */
    suspend fun checkInactivity(userId: String): Boolean {
        val doc = db.collection("users").document(userId).get().await()
        val lastConfirmed = doc.getTimestamp("lastAliveConfirmedAt") ?: return false
        val thresholdDays = doc.getLong("inactivityThresholdDays")?.toInt() ?: 21
        
        val diffMillis = Timestamp.now().toDate().time - lastConfirmed.toDate().time
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
        
        return diffDays >= thresholdDays
    }

    /**
     * Déclenche l'alerte au Dépositaire.
     */
    suspend fun triggerAlert(userId: String) {
        db.collection("users").document(userId)
            .update("protocolStatus", "pending_confirmation")
            .await()
        // Note: Une Cloud Function enverra l'email automatiquement via Firestore Trigger
    }
}
