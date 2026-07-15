package com.example.phoenx.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.preferences.PreferenceManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import com.example.phoenx.data.local.PhoenXDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val encryptionManager: EncryptionManager,
    private val preferenceManager: PreferenceManager,
    private val database: PhoenXDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState

    // ═══ SYSTÈME AVANCÉ EN VEILLE ═══
    // Chiffrement E2EE avec Argon2id + BIP-39
    // Conservé pour activation future (V2 Pro)
    // ══════════════════════════════════════
    /*
    private val _recoveryPhrase = MutableStateFlow<List<String>>(emptyList())
    val recoveryPhrase: StateFlow<List<String>> = _recoveryPhrase

    // Clé de session en mémoire vive uniquement
    var sessionKey: ByteArray? = null
        private set

    fun generateRecoveryPhrase() {
        _recoveryPhrase.value = encryptionManager.generateRecoveryPhrase(context)
    }
    */

    fun login(email: String, password: String) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // Avant toute nouvelle connexion, on vide Room pour éviter les données résiduelles
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Utilisateur introuvable")

                // Vérifier que l'email est confirmé
                if (!user.isEmailVerified) {
                    _uiState.value = AuthState.EmailNotVerified
                    return@launch
                }

                // ETAPE 3 - Récupération de la clé à la connexion
                val userDoc = db.collection("users").document(user.uid).get().await()
                var encryptionKeyBase64 = userDoc.getString("encryptionKey")

                if (encryptionKeyBase64 == null) {
                    // Cas d'un compte créé avant la correction : génération d'une nouvelle clé
                    android.util.Log.w("AuthViewModel", "Compte sans clé de chiffrement, nouvelle clé générée - les anciens souvenirs éventuels resteront indéchiffrables")
                    val newKey = encryptionManager.generateNewSessionKey()
                    encryptionKeyBase64 = android.util.Base64.encodeToString(newKey, android.util.Base64.NO_WRAP)
                    
                    db.collection("users").document(user.uid)
                        .update("encryptionKey", encryptionKeyBase64)
                        .await()
                }

                val decodedKey = android.util.Base64.decode(encryptionKeyBase64, android.util.Base64.NO_WRAP)
                encryptionManager.setSessionKey(decodedKey)

                /*
                // ═══ SYSTÈME AVANCÉ EN VEILLE ═══
                // Charger le sel unique depuis Firestore
                val userDoc = db.collection("users").document(user.uid).get().await()
                val saltBase64 = userDoc.getString("encryptionSalt") 
                    ?: throw Exception("Clé de sécurité corrompue")
                val salt = android.util.Base64.decode(saltBase64, android.util.Base64.DEFAULT)

                // Dérivation de la clé de session avec le sel unique
                val key = encryptionManager.deriveKeyFromPassword(password, salt)
                sessionKey = key
                encryptionManager.setSessionKey(key)
                */
                
                _uiState.value = AuthState.Success
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Erreur de connexion")
            }
        }
    }

    fun signUp(email: String, password: String, birthDate: LocalDate) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // Nettoyage préventif
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: return@launch

                // Envoyer l'email de vérification
                user.sendEmailVerification().await()

                // ETAPE 2 - Écriture de la clé à l'inscription
                val newKey = encryptionManager.generateNewSessionKey()
                val encryptionKeyBase64 = android.util.Base64.encodeToString(newKey, android.util.Base64.NO_WRAP)
                
                val birthDateInstant = birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                
                val userProfile = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "isCreator" to true, // AFFIRMATION EXPLICITE DU RÔLE (Signature 7.6)
                    "encryptionKey" to encryptionKeyBase64,
                    "dateOfBirth" to Timestamp(Date.from(birthDateInstant)),
                    "createdAt" to Timestamp.now(),
                    "onboardingCompleted" to true,
                    "lastAliveConfirmedAt" to Timestamp.now()
                )
                
                db.collection("users").document(user.uid)
                    .set(userProfile, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                // Activer la clé immédiatement pour la session en cours
                encryptionManager.setSessionKey(newKey)

                _uiState.value = AuthState.EmailVerificationSent
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Erreur d'inscription")
            }
        }
    }

    /**
     * Inscription allégée pour les invités (Dépositaires, Témoins, Destinataires).
     * Pas de date de naissance requise, profil minimal.
     */
    fun signUpGuest(email: String, password: String) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // Nettoyage préventif
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: return@launch

                // Envoyer l'email de vérification
                user.sendEmailVerification().await()

                // Génération systématique de la clé AES pour cohérence future
                val newKey = encryptionManager.generateNewSessionKey()
                val encryptionKeyBase64 = android.util.Base64.encodeToString(newKey, android.util.Base64.NO_WRAP)
                
                val userProfile = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "encryptionKey" to encryptionKeyBase64,
                    "createdAt" to Timestamp.now()
                    // signUpGuest reste neutre (isCreator sera défini par la Cloud Function de liaison)
                )
                
                db.collection("users").document(user.uid)
                    .set(userProfile, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                // Activer la clé immédiatement
                encryptionManager.setSessionKey(newKey)

                _uiState.value = AuthState.EmailVerificationSent
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Erreur d'inscription")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = AuthState.PasswordResetSent
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Erreur")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                _uiState.value = AuthState.EmailVerificationSent
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "Erreur")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object EmailVerificationSent : AuthState()
    object EmailNotVerified : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}
