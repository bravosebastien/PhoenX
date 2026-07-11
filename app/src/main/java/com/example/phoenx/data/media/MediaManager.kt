package com.example.phoenx.data.media

import com.example.phoenx.data.encryption.EncryptionManager
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaManager @Inject constructor(
    private val storage: FirebaseStorage,
    private val encryptionManager: EncryptionManager
) {

    /**
     * Chiffre et uploade un fichier vers Firebase Storage.
     * Retourne l'URL de téléchargement.
     */
    suspend fun encryptAndUpload(userId: String, entryId: String, localFile: File): String {
        // 1. Lire le fichier
        val fileBytes = localFile.readBytes()

        // 2. Chiffrer (AES-256-GCM)
        val encryptedBytes = encryptionManager.encryptBytes(fileBytes)

        // 3. Préparer le chemin Storage : users/{uid}/entries/{entryId}/media
        val storageRef = storage.reference
            .child("users")
            .child(userId)
            .child("entries")
            .child(entryId)
            .child(localFile.name + ".enc") // On ajoute l'extension .enc pour indiquer que c'est chiffré

        // 4. Upload
        storageRef.putBytes(encryptedBytes).await()

        // 5. Récupérer l'URL publique (mais le contenu reste illisible sans la clé)
        return storageRef.downloadUrl.await().toString()
    }

    /**
     * Télécharge et déchiffre un média.
     * Retourne les octets bruts (prêts pour affichage/lecture).
     */
    suspend fun downloadAndDecrypt(url: String): ByteArray {
        val storageRef = storage.getReferenceFromUrl(url)
        val encryptedBytes = storageRef.getBytes(Long.MAX_VALUE).await()
        return encryptionManager.decryptBytes(encryptedBytes)
    }
}
