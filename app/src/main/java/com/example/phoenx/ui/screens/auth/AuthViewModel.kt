package com.example.phoenx.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
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
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState

    private val _recoveryPhrase = MutableStateFlow<List<String>>(emptyList())
    val recoveryPhrase: StateFlow<List<String>> = _recoveryPhrase

    fun generateRecoveryPhrase() {
        _recoveryPhrase.value = encryptionManager.generateRecoveryPhrase()
    }

    fun signUp(email: String, password: String, birthDate: LocalDate, depositaryName: String?) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    val salt = "phoenx_permanent_salt".toByteArray()
                    // Simulation de dérivation pour le commit
                    encryptionManager.deriveKeyFromPassword(password, salt)
                    
                    val birthDateInstant = birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    
                    val userProfile = hashMapOf(
                        "uid" to user.uid,
                        "email" to email,
                        "dateOfBirth" to com.google.firebase.Timestamp(Date.from(birthDateInstant)),
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "depositaryName" to depositaryName,
                        "onboardingCompleted" to true
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
