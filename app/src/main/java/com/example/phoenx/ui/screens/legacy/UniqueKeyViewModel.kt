package com.example.phoenx.ui.screens.legacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.LegacyEntity
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UniqueKeyViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UniqueKeyState>(UniqueKeyState.Loading)
    val uiState: StateFlow<UniqueKeyState> = _uiState

    init {
        checkExistingKey()
    }

    private fun checkExistingKey() {
        viewModelScope.launch {
            offlineEntryDao.getAllLegacies().collectLatest { legacies ->
                val existing = legacies.find { it.isUniqueKey }
                if (existing != null) {
                    _uiState.value = UniqueKeyState.AlreadyExists(existing.id)
                } else {
                    _uiState.value = UniqueKeyState.Ready
                }
            }
        }
    }

    fun generateAndSaveKey(recipientId: String, content: String) {
        _uiState.value = UniqueKeyState.Saving
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                // 1. Générer la phrase de 12 mots (BIP-39)
                val phrase = encryptionManager.generateRecoveryPhrase(context)
                val phraseString = phrase.joinToString(" ")
                
                // 2. Chiffrer le contenu avec cette clé unique
                val key = encryptionManager.deriveKeyFromPhrase(phrase)
                val encrypted = encryptionManager.encryptText(content, key)
                val encryptedBase64 = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
                
                // 3. Hasher la phrase pour vérification future (SHA-256)
                val hash = java.security.MessageDigest
                    .getInstance("SHA-256")
                    .digest(phraseString.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                
                // 4. Sauvegarder dans Firestore (Legacies scellés)
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users").document(userId)
                    .collection("legacies").add(mapOf(
                        "recipientId" to recipientId,
                        "encryptedContent" to encryptedBase64,
                        "isUniqueKey" to true,
                        "uniqueKeyHash" to hash,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )).await()

                // 5. Créer l'entité locale pour Offline
                val legacy = LegacyEntity(
                    recipientId = recipientId,
                    entryIds = "",
                    triggerType = "unique_key",
                    isUniqueKey = true,
                    uniqueKeyHash = hash
                )
                
                offlineEntryDao.insertLegacy(legacy)
                _uiState.value = UniqueKeyState.Success(phraseString)
            } catch (e: Exception) {
                _uiState.value = UniqueKeyState.Error(e.message ?: "Erreur")
            }
        }
    }
}

sealed class UniqueKeyState {
    object Loading : UniqueKeyState()
    object Ready : UniqueKeyState()
    object Saving : UniqueKeyState()
    data class Success(val phrase: String) : UniqueKeyState()
    data class AlreadyExists(val id: String) : UniqueKeyState()
    data class Error(val message: String) : UniqueKeyState()
}
