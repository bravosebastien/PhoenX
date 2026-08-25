package com.example.phoenx.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.CreatorProfileEntity
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.data.sync.toFirestoreMap
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
    private val offlineEntryDao: OfflineEntryDao,
    private val preferenceManager: PreferenceManager
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
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // v9.4.27 : Fresh Read de la base locale pour fusionner proprement (Garantie Intégrité)
                val currentLocal = offlineEntryDao.getCreatorProfileSync(userId) ?: updatedProfile
                
                // On prépare l'objet final : Profil de l'UI + Ambiance de la DB locale
                val finalToSave = updatedProfile.copy(
                    transmissionBackgroundId = currentLocal.transmissionBackgroundId,
                    transmissionFontId = currentLocal.transmissionFontId,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "pending"
                )

                // 1. Sauvegarde locale
                offlineEntryDao.insertCreatorProfile(finalToSave)

                // 2. Sauvegarde Firestore immédiate (v9.1)
                // v9.4.27 : Mise à jour PARTIELLE du document racine
                db.collection("users").document(userId)
                    .update(
                        "richProfile", finalToSave.toFirestoreMap(),
                        "transmissionBackgroundId", finalToSave.transmissionBackgroundId,
                        "transmissionFontId", finalToSave.transmissionFontId
                    ).await()
                
                // 3. Mise à jour des préférences locales pour réactivité immédiate (v9.4.29)
                preferenceManager.setGlobalTheme(
                    finalToSave.transmissionBackgroundId,
                    finalToSave.transmissionFontId
                )
                
                offlineEntryDao.insertCreatorProfile(finalToSave.copy(syncStatus = "synced"))
            } catch (e: Exception) {
                android.util.Log.e("CreatorProfile", "Erreur sauvegarde : ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
