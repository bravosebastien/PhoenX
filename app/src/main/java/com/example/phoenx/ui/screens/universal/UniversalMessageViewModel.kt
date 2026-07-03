package com.example.phoenx.ui.screens.universal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class UniversalMessageState {
    object Idle : UniversalMessageState()
    object Loading : UniversalMessageState()
    object AlreadyExists : UniversalMessageState()
    object Success : UniversalMessageState()
    data class Error(val message: String) : UniversalMessageState()
}

@HiltViewModel
class UniversalMessageViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<UniversalMessageState>(UniversalMessageState.Idle)
    val uiState: StateFlow<UniversalMessageState> = _uiState.asStateFlow()

    fun checkExistingMessage(userId: String) {
        viewModelScope.launch {
            _uiState.value = UniversalMessageState.Loading
            try {
                val existing = db.collection("universalMessages")
                    .whereEqualTo("creatorId", userId)
                    .limit(1)
                    .get()
                    .await()
                if (!existing.isEmpty) {
                    _uiState.value = UniversalMessageState.AlreadyExists
                } else {
                    _uiState.value = UniversalMessageState.Idle
                }
            } catch (e: Exception) {
                _uiState.value = UniversalMessageState.Error(e.message ?: "Erreur de vérification")
            }
        }
    }
}
