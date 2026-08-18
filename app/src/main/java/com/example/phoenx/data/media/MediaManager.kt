package com.example.phoenx.data.media

import com.example.phoenx.data.encryption.EncryptionManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions
import androidx.media3.datasource.DataSource
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaManager @Inject constructor(
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
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
        return storageRef.path.removePrefix("/")
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
        return storageRef.path.removePrefix("/")
    }

    /**
     * Chiffre et uploade un fichier pour le module "Lien Vivant" (v9.4.27).
     * RÈGLE : Dossier dédié pour isolation totale.
     */
    suspend fun uploadLivingLinkFile(userId: String, snapshotId: String, localFile: File): String {
        val fileBytes = localFile.readBytes()
        val encryptedBytes = encryptionManager.encryptBytes(fileBytes)

        val storageRef = storage.reference
            .child("users")
            .child(userId)
            .child("living_links")
            .child(snapshotId + ".enc")

        storageRef.putBytes(encryptedBytes).await()
        return storageRef.path.removePrefix("/")
    }

    /**
     * Télécharge et déchiffre un média.
     * Supporte URLs héritées, Chemins Storage et Résolution via Cloud Function (v9.4.27).
     * Gère le téléchargement direct HTTP pour les URLs signées Google Cloud (v9.4.27).
     */
    suspend fun downloadAndDecrypt(
        pathOrUrl: String, 
        explicitKey: ByteArray? = null,
        creatorId: String? = null,
        docType: String? = null,
        docId: String? = null,
        field: String? = null // v9.4.27
    ): ByteArray {
        var isSignedUrl = false
        val finalUrl = if (explicitKey != null && creatorId != null && docType != null && docId != null) {
            // MODE DESTINATAIRE : Résolution sécurisée via Cloud Function (Signature v9.4.27)
            try {
                val params = mutableMapOf(
                    "creatorId" to creatorId,
                    "docType" to docType,
                    "docId" to docId
                )
                if (field != null) params["field"] = field

                val result = functions.getHttpsCallable("getInheritedFileUrl").call(params).await()
                val data = result.data as? Map<*, *>
                val url = data?.get("url") as? String
                if (url != null) {
                    isSignedUrl = true
                    url
                } else pathOrUrl
            } catch (e: Exception) {
                android.util.Log.e("MediaManager", "Échec getInheritedFileUrl pour $docId", e)
                pathOrUrl
            }
        } else {
            // MODE CRÉATEUR ou Cas non spécifié : Accès direct Storage (Performance maximale)
            pathOrUrl
        }

        val encryptedBytes = if (isSignedUrl) {
            // Téléchargement HTTP direct pour les URLs signées (Contourne l'erreur SDK Storage v9.4.27)
            withContext(Dispatchers.IO) {
                java.net.URL(finalUrl).readBytes()
            }
        } else {
            // Accès via SDK Firebase Storage (Chemins internes ou URLs Firebase avec Token)
            val storageRef = if (finalUrl.startsWith("http")) {
                storage.getReferenceFromUrl(finalUrl)
            } else {
                storage.getReference(finalUrl)
            }
            storageRef.getBytes(Long.MAX_VALUE).await()
        }

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
        return storageRef.path.removePrefix("/")
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
