package com.example.phoenx.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    init {
        loadCovers()
    }

    fun loadCovers() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId).collection("libraryCover").get().await()
                val coverMap = snapshot.documents.mapNotNull { 
                    it.toObject(LibraryCover::class.java) 
                }.associateBy { it.compartmentId }
                _covers.value = coverMap
            } catch (e: Exception) {
                // Erreur silencieuse
            }
        }
    }

    fun uploadCover(compartmentId: String, uri: Uri, mediaType: String) {
        val userId = auth.currentUser?.uid ?: return
        val extension = if (mediaType == "video") "mp4" else "jpg"
        val ref = storage.reference.child("users/$userId/library_covers/$compartmentId.$extension")

        _isUploading.value = true
        _uploadProgress.value = 0f

        val uploadTask = ref.putFile(uri)
        
        uploadTask.addOnProgressListener { snapshot ->
            val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
            _uploadProgress.value = progress
        }.addOnSuccessListener {
            viewModelScope.launch {
                val downloadUrl = ref.downloadUrl.await().toString()
                val cover = LibraryCover(
                    compartmentId = compartmentId,
                    mediaType = mediaType,
                    mediaUrl = downloadUrl,
                    uploadedAt = System.currentTimeMillis()
                )
                db.collection("users").document(userId)
                    .collection("libraryCover").document(compartmentId)
                    .set(cover).await()
                
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
                // On tente de supprimer les deux extensions possibles par précaution
                val photoRef = storage.reference.child("users/$userId/library_covers/$compartmentId.jpg")
                val videoRef = storage.reference.child("users/$userId/library_covers/$compartmentId.mp4")
                
                photoRef.delete().addOnCompleteListener { /* ignore results */ }
                videoRef.delete().addOnCompleteListener { /* ignore results */ }

                db.collection("users").document(userId)
                    .collection("libraryCover").document(compartmentId)
                    .delete().await()

                loadCovers()
            } catch (e: Exception) {
                // Erreur
            }
        }
    }
}
