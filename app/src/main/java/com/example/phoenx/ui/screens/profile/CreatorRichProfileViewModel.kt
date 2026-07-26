package com.example.phoenx.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.CreatorProfileEntity
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.toFirestoreMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CreatorRichProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _profile = MutableStateFlow<CreatorProfileEntity?>(null)
    val profile: StateFlow<CreatorProfileEntity?> = _profile.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            offlineEntryDao.getCreatorProfile(userId).collect { localProfile ->
                if (localProfile != null) {
                    _profile.value = localProfile
                } else {
                    // Si pas de profil local, on tente de le créer par défaut
                    val newProfile = CreatorProfileEntity(userId = userId)
                    offlineEntryDao.insertCreatorProfile(newProfile)
                    _profile.value = newProfile
                }
            }
        }
    }

    fun updateProfile(updatedProfile: CreatorProfileEntity) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // 1. Sauvegarde locale
                offlineEntryDao.insertCreatorProfile(updatedProfile.copy(
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "pending"
                ))

                // 2. Sauvegarde Firestore immédiate (v9.1)
                val userId = auth.currentUser?.uid ?: return@launch
                db.collection("users").document(userId)
                    .update("richProfile", updatedProfile.toFirestoreMap())
                    .await()
                
                offlineEntryDao.insertCreatorProfile(updatedProfile.copy(syncStatus = "synced"))
            } catch (e: Exception) {
                android.util.Log.e("CreatorProfile", "Erreur sauvegarde : ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
