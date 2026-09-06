package com.example.phoenx.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LibraryCoverViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _covers = MutableStateFlow<Map<String, LibraryCover>>(emptyMap())
    val covers: StateFlow<Map<String, LibraryCover>> = _covers.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val registrations = mutableListOf<ListenerRegistration>()
    private val coversMap = mutableMapOf<String, LibraryCover>()

    init {
        loadCovers()
    }

    fun loadCovers() {
        val userId = auth.currentUser?.uid ?: return
        
        // Nettoyage des anciens écouteurs si appel répété
        clearRegistrations()

        // 1. Écouteur pour la couverture du Livre (BIBLIOTHEQUE)
        val bookReg = db.collection("users").document(userId)
            .collection("book").document("current_draft")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists() && snapshot.contains("coverImageUrl")) {
                    val cover = LibraryCover(
                        compartmentId = "BIBLIOTHEQUE",
                        mediaType = "photo",
                        mediaUrl = snapshot.getString("coverImageUrl") ?: "",
                        scale = snapshot.getDouble("coverScale")?.toFloat() ?: 1f,
                        offsetX = snapshot.getDouble("coverOffsetX")?.toFloat() ?: 0f,
                        offsetY = snapshot.getDouble("coverOffsetY")?.toFloat() ?: 0f
                    )
                    updateLocalMap("BIBLIOTHEQUE", cover)
                }
            }
        registrations.add(bookReg)

        // 2. Écouteur pour les autres compartiments
        val collReg = db.collection("users").document(userId)
            .collection("libraryCover")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.documents?.forEach { doc ->
                    val c = doc.toObject(LibraryCover::class.java)
                    if (c != null && c.compartmentId != "BIBLIOTHEQUE") {
                        updateLocalMap(c.compartmentId, c)
                    }
                }
            }
        registrations.add(collReg)
    }

    private fun updateLocalMap(id: String, cover: LibraryCover) {
        coversMap[id] = cover
        _covers.value = coversMap.toMap()
    }

    private fun clearRegistrations() {
        registrations.forEach { it.remove() }
        registrations.clear()
    }

    override fun onCleared() {
        super.onCleared()
        clearRegistrations()
    }

    fun uploadCover(compartmentId: String, uri: Uri, mediaType: String, scale: Float = 1f, offsetX: Float = 0f, offsetY: Float = 0f) {
        val userId = auth.currentUser?.uid ?: return
        val extension = if (mediaType == "video") "mp4" else "jpg"
        val path = "users/$userId/library_covers/$compartmentId.$extension"
        val ref = storage.reference.child(path)

        _isUploading.value = true
        _uploadProgress.value = 0f

        val uploadTask = ref.putFile(uri)
        
        uploadTask.addOnProgressListener { snapshot ->
            val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
            _uploadProgress.value = progress
        }.addOnSuccessListener {
            viewModelScope.launch {
                if (compartmentId == "BIBLIOTHEQUE") {
                    // Unification sur current_draft (v9.4.19)
                    db.collection("users").document(userId)
                        .collection("book").document("current_draft")
                        .update(mapOf(
                            "coverImageUrl" to path,
                            "coverScale" to scale,
                            "coverOffsetX" to offsetX,
                            "coverOffsetY" to offsetY,
                            "coverUploadedAt" to System.currentTimeMillis()
                        )).await()
                } else {
                    val cover = LibraryCover(
                        compartmentId = compartmentId,
                        mediaType = mediaType,
                        mediaUrl = path,
                        uploadedAt = System.currentTimeMillis(),
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                    db.collection("users").document(userId)
                        .collection("libraryCover").document(compartmentId)
                        .set(cover).await()
                }
                
                loadCovers()
                _isUploading.value = false
            }
        }.addOnFailureListener {
            _isUploading.value = false
        }
    }

    fun deleteCover(compartmentId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val photoRef = storage.reference.child("users/$userId/library_covers/$compartmentId.jpg")
                val videoRef = storage.reference.child("users/$userId/library_covers/$compartmentId.mp4")
                
                photoRef.delete().addOnCompleteListener { }
                videoRef.delete().addOnCompleteListener { }

                if (compartmentId == "BIBLIOTHEQUE") {
                    db.collection("users").document(userId)
                        .collection("book").document("current_draft")
                        .update(mapOf(
                            "coverImageUrl" to com.google.firebase.firestore.FieldValue.delete(),
                            "coverScale" to com.google.firebase.firestore.FieldValue.delete(),
                            "coverOffsetX" to com.google.firebase.firestore.FieldValue.delete(),
                            "coverOffsetY" to com.google.firebase.firestore.FieldValue.delete()
                        )).await()
                } else {
                    db.collection("users").document(userId)
                        .collection("libraryCover").document(compartmentId)
                        .delete().await()
                }

                loadCovers()
            } catch (e: Exception) { }
        }
    }

    fun updateCoverMetadata(compartmentId: String, scale: Float, offsetX: Float, offsetY: Float) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                if (compartmentId == "BIBLIOTHEQUE") {
                    db.collection("users").document(userId)
                        .collection("book").document("current_draft")
                        .update(mapOf(
                            "coverScale" to scale,
                            "coverOffsetX" to offsetX,
                            "coverOffsetY" to offsetY
                        )).await()
                } else {
                    db.collection("users").document(userId)
                        .collection("libraryCover").document(compartmentId)
                        .update(mapOf(
                            "scale" to scale,
                            "offsetX" to offsetX,
                            "offsetY" to offsetY
                        )).await()
                }
                loadCovers()
            } catch (e: Exception) {}
        }
    }
}
