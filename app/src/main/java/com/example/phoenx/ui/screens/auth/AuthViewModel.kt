package com.example.phoenx.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.preferences.PreferenceManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val encryptionManager: EncryptionManager,
    private val preferenceManager: PreferenceManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState

    private val _recoveryPhrase = MutableStateFlow<List<String>>(emptyList())
    val recoveryPhrase: StateFlow<List<String>> = _recoveryPhrase

    // Clé de session en mémoire vive uniquement
    var sessionKey: ByteArray? = null
        private set

    fun generateRecoveryPhrase() {
        _recoveryPhrase.value = encryptionManager.generateRecoveryPhrase(context)
    }

    fun login(email: String, password: String) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("Utilisateur introuvable")

                // Charger le sel unique depuis Firestore
                val userDoc = db.collection("users").document(user.uid).get().await()
                val saltBase64 = userDoc.getString("encryptionSalt") 
                    ?: throw Exception("Clé de sécurité corrompue")
                val salt = android.util.Base64.decode(saltBase64, android.util.Base64.DEFAULT)

                // Dérivation de la clé de session avec le sel unique
                val key = encryptionManager.deriveKeyFromPassword(password, salt)
                sessionKey = key
                encryptionManager.setSessionKey(key)
                
                _uiState.value = AuthState.Success
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Erreur de connexion")
            }
        }
    }

    fun signUp(email: String, password: String, birthDate: LocalDate, depositaryName: String?) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    // Générer un sel aléatoire unique de 32 bytes
                    val salt = ByteArray(32)
                    java.security.SecureRandom().nextBytes(salt)
                    val saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT)

                    // Dériver la clé avec ce sel
                    val key = encryptionManager.deriveKeyFromPassword(password, salt)
                    sessionKey = key
                    encryptionManager.setSessionKey(key)

                    // Sauvegarder la phrase de récupération localement
                    val phraseString = _recoveryPhrase.value.joinToString(" ")
                    preferenceManager.setRecoveryPhrase(phraseString)
                    preferenceManager.updateLastRecoveryReminder(System.currentTimeMillis())
                    
                    val birthDateInstant = birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    
                    val userProfile = hashMapOf(
                        "uid" to user.uid,
                        "email" to email,
                        "encryptionSalt" to saltBase64, // Stockage du sel unique
                        "dateOfBirth" to Timestamp(Date.from(birthDateInstant)),
                        "createdAt" to Timestamp.now(),
                        "depositaryName" to depositaryName,
                        "onboardingCompleted" to true,
                        "lastAliveConfirmedAt" to Timestamp.now()
                    )
                    
                    db.collection("users").document(user.uid).set(userProfile).await()
                    _uiState.value = AuthState.Success
                }
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Erreur d'inscription")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
