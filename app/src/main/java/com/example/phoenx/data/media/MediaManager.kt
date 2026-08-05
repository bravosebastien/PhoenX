package com.example.phoenx.data.media

import com.example.phoenx.data.encryption.EncryptionManager
import com.google.firebase.storage.FirebaseStorage
import androidx.media3.datasource.DataSource
import androidx.media3.common.util.UnstableApi
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
     * Entry Point pour accès depuis les Composables (v9.4.17)
     */
    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface MediaManagerEntryPoint {
        fun mediaManager(): MediaManager
    }

    /**
     * Résout un chemin Storage ou une ancienne URL en URL de téléchargement (v9.4.17).
     * Utilisé uniquement pour l'affichage en mémoire (Coil/ExoPlayer).
     */
    suspend fun getSafeUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        if (pathOrUrl.startsWith("http")) return pathOrUrl
        
        // v9.4.24: Support des chemins locaux (Preview immédiate avant upload)
        if (pathOrUrl.startsWith("/") || pathOrUrl.contains("/app.phoenx.mobile/files/")) {
            val file = File(pathOrUrl)
            if (file.exists()) return "file://$pathOrUrl"
        }

        return try {
            storage.getReference(pathOrUrl).downloadUrl.await().toString()
        } catch (e: Exception) {
            // Repli de secours : si la résolution Storage échoue mais que c'est un chemin local valide
            val file = File(pathOrUrl)
            if (file.exists()) return "file://$pathOrUrl"

            android.util.Log.e("MediaManager", "Erreur résolution chemin : $pathOrUrl", e)
            null
        }
    }

    /**
     * Chiffre et uploade un fichier vers Firebase Storage.
     * Retourne le CHEMIN Storage (v9.4.17).
     */
    suspend fun encryptAndUpload(userId: String, entryId: String, localFile: File): String {
        val fileBytes = localFile.readBytes()
        val encryptedBytes = encryptionManager.encryptBytes(fileBytes)

        val storageRef = storage.reference
            .child("users")
            .child(userId)
            .child("entries")
            .child(entryId)
            .child(localFile.name + ".enc")

        storageRef.putBytes(encryptedBytes).await()
        return storageRef.path
    }

    /**
     * Chiffre et uploade une photo Standalone vers Firebase Storage (v9.4.17).
     */
    suspend fun encryptAndUploadStandalone(userId: String, mediaId: String, localFile: File): String {
        val fileBytes = localFile.readBytes()
        val encryptedBytes = encryptionManager.encryptBytes(fileBytes)

        val storageRef = storage.reference
            .child("users")
            .child(userId)
            .child("standalone_photos")
            .child("$mediaId.jpg.enc")

        storageRef.putBytes(encryptedBytes).await()
        return storageRef.path
    }

    /**
     * Télécharge et déchiffre un média.
     * Supporte URLs héritées et Chemins Storage (v9.4.17).
     */
    suspend fun downloadAndDecrypt(pathOrUrl: String, explicitKey: ByteArray? = null): ByteArray {
        val storageRef = if (pathOrUrl.startsWith("http")) {
            storage.getReferenceFromUrl(pathOrUrl)
        } else {
            storage.getReference(pathOrUrl)
        }
        val encryptedBytes = storageRef.getBytes(Long.MAX_VALUE).await()
        return encryptionManager.decryptBytes(encryptedBytes, explicitKey)
    }

    /**
     * Uploade un portrait Cameo vers Firebase Storage.
     * Retourne le CHEMIN Storage (v9.4.17).
     */
    suspend fun uploadCameo(userId: String, personId: String, localFile: File): String {
        val storageRef = storage.reference
            .child("users")
            .child(userId)
            .child("cameos")
            .child("$personId.jpg")

        storageRef.putFile(android.net.Uri.fromFile(localFile)).await()
        return storageRef.path
    }

    /**
     * Télécharge un portrait Cameo depuis Storage (v9.4.17).
     */
    suspend fun downloadCameo(pathOrUrl: String, destFile: File) {
        val storageRef = if (pathOrUrl.startsWith("http")) {
            storage.getReferenceFromUrl(pathOrUrl)
        } else {
            storage.getReference(pathOrUrl)
        }
        storageRef.getFile(destFile).await()
    }

    /**
     * Supprime un fichier du Firebase Storage (v9.4.27)
     */
    suspend fun deleteFile(pathOrUrl: String?) {
        if (pathOrUrl.isNullOrBlank()) return
        if (pathOrUrl.startsWith("/")) return // Chemin local, pas de suppression Storage

        try {
            val storageRef = if (pathOrUrl.startsWith("http")) {
                storage.getReferenceFromUrl(pathOrUrl)
            } else {
                storage.getReference(pathOrUrl)
            }
            storageRef.delete().await()
            android.util.Log.d("MediaManager", "Fichier Storage supprimé : $pathOrUrl")
        } catch (e: Exception) {
            android.util.Log.w("MediaManager", "Échec suppression Storage (peut-être déjà supprimé) : $pathOrUrl")
        }
    }

    /**
     * Fournit une factory de source de données pour ExoPlayer (Streaming Chiffré).
     */
    @UnstableApi
    fun getEncryptedDataSourceFactory(explicitKey: ByteArray? = null): androidx.media3.datasource.DataSource.Factory {
        return EncryptedMediaDataSourceFactory(encryptionManager, explicitKey)
    }
}
