package com.example.phoenx.ui.screens.depositary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.domain.models.SilenceConfig
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DepositaryViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepositaryUiState())
    val uiState: StateFlow<DepositaryUiState> = _uiState.asStateFlow()

    fun loadCreatorStatus(creatorId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(creatorId).get().await()
                val name = doc.getString("displayName") ?: "Proche"
                val missedCycles = doc.getLong("silenceConfig.missedCycles")?.toInt() ?: 0
                val lastCheckInAt = doc.getTimestamp("silenceConfig.lastCheckInAt")
                
                val daysSince = if (lastCheckInAt != null) {
                    (System.currentTimeMillis() - lastCheckInAt.toDate().time) / (1000 * 60 * 60 * 24)
                } else 0
                
                _uiState.update { it.copy(
                    creatorName = name,
                    missedCycles = missedCycles,
                    daysSinceLastCheckIn = daysSince.toInt(),
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class DepositaryUiState(
    val creatorName: String = "",
    val missedCycles: Int = 0,
    val daysSinceLastCheckIn: Int = 0,
    val isLoading: Boolean = true
)
