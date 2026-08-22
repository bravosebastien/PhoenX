package com.example.phoenx.data.memory

import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.media.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class DeleteOutcome {
    object Success : DeleteOutcome()
    data class Failure(val message: String) : DeleteOutcome()
}

/**
 * Gère la suppression d'un souvenir (et de ses compléments) ou d'un simple complément.
 * Extrait de MemoryDetailViewModel — étape 4/7 du découpage.
 */
@Singleton
class MemoryDeletionManager @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val mediaManager: MediaManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    suspend fun deleteEntryById(id: String, isParent: Boolean): DeleteOutcome {
        val uid = auth.currentUser?.uid
            ?: return DeleteOutcome.Failure("Utilisateur non connecté")

        return try {
            if (isParent) {
                val parent = offlineEntryDao.getEntryById(id).first()
                val children = offlineEntryDao.getComplements(id).first()

                parent?.let {
                    mediaManager.deleteFile(it.mediaUrl)
                    mediaManager.deleteFile(it.coverUrl)
                }
                children.forEach { child ->
                    mediaManager.deleteFile(child.mediaUrl)
                    mediaManager.deleteFile(child.coverUrl)
                }

                val batch = db.batch()
                val userRef = db.collection("users").document(uid)

                batch.delete(userRef.collection("entries").document(id))
                children.forEach { child ->
                    batch.delete(userRef.collection("entries").document(child.id))
                }
                batch.commit().await()

                offlineEntryDao.deleteEntry(id)
                children.forEach { child ->
                    offlineEntryDao.deleteEntry(child.id)
                }

                DeleteOutcome.Success
            } else {
                val complement = offlineEntryDao.getEntryById(id).first()
                complement?.let {
                    mediaManager.deleteFile(it.mediaUrl)
                    mediaManager.deleteFile(it.coverUrl)
                }

                db.collection("users").document(uid)
                    .collection("entries").document(id)
                    .delete()
                    .await()
                offlineEntryDao.deleteEntry(id)

                DeleteOutcome.Success
            }
        } catch (e: Exception) {
            android.util.Log.e("MemoryDetailVM", "Erreur lors de la suppression de $id", e)
            DeleteOutcome.Failure("Échec de la suppression : ${e.message}")
        }
    }
}