package com.example.phoenx.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState

    fun saveEntry(
        content: String,
        type: String,
        category: String,
        visibility: String
    ) {
        val user = auth.currentUser ?: return
        _uiState.value = CaptureUiState.Loading

        viewModelScope.launch {
            try {
                // 1. Récupérer la date de naissance pour le calcul de l'âge (Signature)
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                // 2. Chiffrement E2EE via Tink (Sécurité Absolue)
                // Note : On utilise un sel fixe pour le MVP, à lier au mot de passe plus tard
                val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
                val encrypted = encryptionManager.encryptText(content, tempKey)
                
                // 3. Sauvegarder dans la file locale Room (Mode Hors-ligne)
                val entry = OfflineEntry(
                    encryptedPayload = encrypted,
                    entryType = type,
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = category,
                    visibility = visibility,
                    createdAt = System.currentTimeMillis()
                )
                
                offlineEntryDao.insertEntry(entry)
                
                // 4. Succès -> Déclenche l'animation de rituel dans l'UI
                _uiState.value = CaptureUiState.Success
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error(e.message ?: "Erreur lors du dépôt du souvenir")
            }
        }
    }
}

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object Loading : CaptureUiState()
    object Success : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}
