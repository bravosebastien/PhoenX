package com.example.phoenx.data.repository

import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.StandaloneMediaDao
import com.example.phoenx.data.local.StandaloneMediaEntity
import com.example.phoenx.data.model.StandaloneMedia
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandaloneMediaRepository @Inject constructor(
    private val standaloneMediaDao: StandaloneMediaDao,
    private val encryptionManager: EncryptionManager,
    private val auth: FirebaseAuth
) {
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    /**
     * Sauvegarde un nouveau média Standalone (v9.3.2)
     * Applique le chiffrement si nécessaire.
     */
    suspend fun saveMedia(media: StandaloneMedia) {
        val needsEncryption = media.type == "TEXT_EXCERPT" || media.type == "PHOTO"
        
        val finalContent = if (needsEncryption) {
            val encrypted = encryptionManager.encryptText(media.content)
            android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        } else {
            media.content // SPOTIFY, YOUTUBE -> En clair
        }

        val entity = StandaloneMediaEntity(
            id = media.id,
            creatorUid = currentUid,
            type = media.type,
            title = media.title,
            content = finalContent,
            recipientIds = media.recipientIds.joinToString(","),
            createdAt = media.createdAt,
            syncStatus = "pending"
        )

        standaloneMediaDao.insertMedia(entity)
    }

    fun getMediaByType(type: String): Flow<List<StandaloneMedia>> {
        return standaloneMediaDao.getMediaByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun StandaloneMediaEntity.toDomain(): StandaloneMedia {
        val needsDecryption = type == "TEXT_EXCERPT" || type == "PHOTO"
        
        val decryptedContent = if (needsDecryption) {
            try {
                val bytes = android.util.Base64.decode(content, android.util.Base64.DEFAULT)
                encryptionManager.decryptText(bytes)
            } catch (e: Exception) {
                "Contenu chiffré"
            }
        } else {
            content
        }

        return StandaloneMedia(
            id = id,
            type = type,
            title = title,
            content = decryptedContent,
            recipientIds = recipientIds.split(",").filter { it.isNotBlank() },
            createdAt = createdAt
        )
    }
}
