package com.example.phoenx.data.living

import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.media.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Blob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LivingLinkService (v9.4.27) — Gère la transmission active isolée.
 * RÈGLE D'OR : Indépendant du protocole de décès.
 */
@Singleton
class LivingLinkService @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val encryptionManager: EncryptionManager,
    private val mediaManager: MediaManager,
    private val offlineEntryDao: OfflineEntryDao
) {
    suspend fun sendLivingLink(
        entryId: String,
        recipientUid: String,
        scheduledAt: Long? = null
    ): String {
        val userId = auth.currentUser?.uid ?: throw Exception("Non authentifié")
        
        // 1. Récupération et déchiffrement local du souvenir
        val entry = offlineEntryDao.getEntryById(entryId).firstOrNull() ?: throw Exception("Souvenir introuvable")
        val originalText = encryptionManager.decryptText(entry.encryptedPayload)
        
        // 2. Récupération de la clé publique du destinataire
        val recipientDoc = db.collection("users").document(recipientUid).get().await()
        val publicKeyBase64 = recipientDoc.getString("publicKey") ?: throw Exception("Destinataire incompatible (clé manquante)")
        val publicKeyBytes = android.util.Base64.decode(publicKeyBase64, android.util.Base64.DEFAULT)

        // 3. Snapshot cryptographique (Isolation AES)
        val linkKey = encryptionManager.generateNewSessionKey()
        val encryptedContent = encryptionManager.encryptText(originalText, linkKey)
        
        // 4. Chiffrement de la clé pour le destinataire (RSA)
        val encryptedLinkKey = encryptionManager.encryptWithPublicKey(
            android.util.Base64.encodeToString(linkKey, android.util.Base64.NO_WRAP), 
            publicKeyBytes
        )

        // 5. Duplication des médias (Storage)
        val mediaPaths = mutableListOf<String>()
        if (!entry.localMediaPath.isNullOrEmpty()) {
            val localFile = File(entry.localMediaPath)
            if (localFile.exists()) {
                val snapshotId = UUID.randomUUID().toString()
                // On uploade une COPIE dans un dossier dédié living_links
                val storagePath = mediaManager.uploadLivingLinkFile(userId, snapshotId, localFile)
                mediaPaths.add(storagePath)
            }
        }

        // 6. Création du document Firestore racine
        val linkId = UUID.randomUUID().toString()
        val data = hashMapOf(
            "creatorId" to userId,
            "recipientId" to recipientUid,
            "type" to entry.entryType,
            "status" to if (scheduledAt == null) "sent" else "pending",
            "scheduledAt" to scheduledAt?.let { com.google.firebase.Timestamp(it / 1000, 0) },
            "sentAt" to if (scheduledAt == null) com.google.firebase.Timestamp.now() else null,
            "encryptedContent" to Blob.fromBytes(encryptedContent),
            "encryptedLinkKey" to Blob.fromBytes(encryptedLinkKey),
            "mediaUrls" to mediaPaths,
            "originalEntryId" to entryId,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("livingLinks").document(linkId).set(data).await()
        return linkId
    }
}
