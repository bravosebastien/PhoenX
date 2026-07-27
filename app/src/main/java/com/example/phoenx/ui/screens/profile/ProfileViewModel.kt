package com.example.phoenx.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    private val storage: FirebaseStorage
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
                val photo = doc.getString("photoUrl")
                _uiState.update { it.copy(displayName = name, photoUrl = photo, isLoading = false) }
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
                    val downloadUrl = ref.downloadUrl.await().toString()
                    db.collection("users").document(userId)
                        .set(mapOf("photoUrl" to downloadUrl), SetOptions.merge())
                        .await()
                    
                    _uiState.update { it.copy(photoUrl = downloadUrl, isLoading = false) }
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
}
