package com.example.phoenx.ui.screens.universal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UniversalJoinUiState(
    val isLoading: Boolean = false,
    val invitation: InvitationDetails? = null,
    val error: String? = null,
    val success: Boolean = false
)

data class InvitationDetails(
    val creatorName: String,
    val creatorId: String,
    val role: String,
    val label: String,
    val targetEmail: String
)

@HiltViewModel
class UniversalJoinViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniversalJoinUiState())
    val uiState: StateFlow<UniversalJoinUiState> = _uiState.asStateFlow()

    fun loadInvitation(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = functions.getHttpsCallable("getInvitationDetails")
                    .call(mapOf("tokenId" to token))
                    .await()
                
                val data = result.data as Map<*, *>
                _uiState.update { it.copy(
                    isLoading = false,
                    invitation = InvitationDetails(
                        creatorName = data["creatorName"] as String,
                        creatorId = data["creatorId"] as String,
                        role = data["role"] as String,
                        label = data["label"] as String,
                        targetEmail = data["targetEmail"] as String
                    )
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Cette invitation est introuvable ou a expiré."
                ) }
            }
        }
    }

    fun acceptInvitation(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                functions.getHttpsCallable("acceptUniversalInvitation")
                    .call(mapOf("tokenId" to token))
                    .await()
                
                _uiState.update { it.copy(isLoading = false, success = true) }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("already-exists") == true -> "Cette invitation a déjà été utilisée."
                    e.message?.contains("permission-denied") == true -> "Désolé, cette invitation n'est pas destinée à ce compte."
                    else -> "Une erreur est survenue lors de la liaison."
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }
}
