package com.example.phoenx.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.media.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class DeleteResult {
    object Success : DeleteResult()
    object RequiresReauth : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val mediaManager: MediaManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        _uiState.update { it.copy(isLoading = true, email = user.email ?: "") }
        
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(user.uid).get().await()
                val name = doc.getString("displayName") ?: ""
                val photoPath = doc.getString("photoUrl")
                
                // Résolution v9.4.17
                val photoUrl = mediaManager.getSafeUrl(photoPath)
                
                _uiState.update { it.copy(displayName = name, photoUrl = photoUrl, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid)
                    .set(mapOf("displayName" to newName), SetOptions.merge())
                    .await()
                
                _uiState.update { it.copy(displayName = newName) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateProfilePhoto(uri: android.net.Uri) {
        val userId = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("users/$userId/profile_photo.jpg")

        _uiState.update { it.copy(isLoading = true) }
        _uploadProgress.value = 0f

        val uploadTask = ref.putFile(uri)
        
        uploadTask.addOnProgressListener { snapshot ->
            val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
            _uploadProgress.value = progress
        }.addOnSuccessListener {
            viewModelScope.launch {
                try {
                    // Stockage du CHEMIN (v9.4.17)
                    val storagePath = ref.path
                    db.collection("users").document(userId)
                        .set(mapOf("photoUrl" to storagePath), SetOptions.merge())
                        .await()
                    
                    // Résolution pour affichage immédiat
                    val displayUrl = mediaManager.getSafeUrl(storagePath)
                    
                    _uiState.update { it.copy(photoUrl = displayUrl, isLoading = false) }
                    _uploadProgress.value = null
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _uploadProgress.value = null
                }
            }
        }.addOnFailureListener { e ->
            _uiState.update { it.copy(isLoading = false, error = e.message) }
            _uploadProgress.value = null
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun suspendAccount(onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onComplete()
            return
        }

        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update(mapOf(
                        "accountStatus" to "suspended",
                        "suspendedAt" to FieldValue.serverTimestamp()
                    )).await()
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Erreur lors de la suspension: ${e.message}")
            }
            onComplete()
        }
    }

    fun deleteAccount(onResult: (DeleteResult) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(DeleteResult.Error("Aucun utilisateur connecté"))
            return
        }
        user.delete()
            .addOnSuccessListener { onResult(DeleteResult.Success) }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    onResult(DeleteResult.RequiresReauth)
                } else {
                    onResult(DeleteResult.Error(e.message ?: "Erreur inconnue"))
                }
            }
    }
}
