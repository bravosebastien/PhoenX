package com.example.phoenx.ui.screens.universal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val success: Boolean = false,
    val acceptedRole: String? = null,
    val creatorId: String? = null,
    val sourceId: String? = null
)

data class InvitationDetails(
    val creatorName: String,
    val creatorId: String,
    val role: String,
    val label: String,
    val targetEmail: String,
    val sourceId: String? = null
)

@HiltViewModel
class UniversalJoinViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val analyticsTracker: com.example.phoenx.data.analytics.AnalyticsTracker,
    @ApplicationContext private val context: Context
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
                        targetEmail = data["targetEmail"] as String,
                        sourceId = data["sourceId"] as? String
                    )
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = context.getString(R.string.universal_join_error_not_found)
                ) }
            }
        }
    }

    fun acceptInvitation(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = functions.getHttpsCallable("acceptUniversalInvitation")
                    .call(mapOf("tokenId" to token))
                    .await()
                
                val resData = result.data as Map<*, *>
                val role = resData["role"] as? String
                val invitation = _uiState.value.invitation

                analyticsTracker.logInvitationAccepted(role ?: "unknown")

                _uiState.update { it.copy(
                    isLoading = false, 
                    success = true,
                    acceptedRole = role,
                    creatorId = invitation?.creatorId,
                    sourceId = invitation?.sourceId
                ) }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("already-exists") == true -> context.getString(R.string.universal_join_error_already_used)
                    e.message?.contains("permission-denied") == true -> context.getString(R.string.universal_join_error_wrong_account)
                    else -> context.getString(R.string.universal_join_error_generic)
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }
}
