package com.example.phoenx.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class ActivationProtocolManager @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun confirmProofOfLife(userId: String) {
        db.collection("users").document(userId)
            .update("lastAliveConfirmedAt", com.google.firebase.Timestamp.now())
            .await()
    }

    suspend fun getProtocolStatus(userId: String): String {
        val doc = db.collection("users").document(userId).get().await()
        return doc.getString("protocolStatus") ?: "dormant"
    }
}
