package com.example.phoenx.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class StepByStepUiState(
    val currentStep: Int = 1,
    val title: String = "",
    val category: String = "Sagesse",
    val memoryDate: Long? = null,
    val memoryDateStart: Long? = null,
    val memoryDateEnd: Long? = null,
    val isPeriodMode: Boolean = false,
    val locationId: String? = null,
    val locationName: String? = null,
    
    // v9.4.27 : Coffre-Fort
    val enigmaEnabled: Boolean = false,
    val enigmaQuestion: String = "",
    val enigmaAnswer: String = "",
    val enigmaHint: String = "",
    val autoUnlockDays: Int? = 30,
    val isUltimateSecret: Boolean = false
)

@HiltViewModel
class StepByStepCaptureViewModel @Inject constructor(
    private val db: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepByStepUiState())
    val uiState: StateFlow<StepByStepUiState> = _uiState.asStateFlow()

    fun nextStep() {
        _uiState.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun updateMemoryDate(date: Long?) {
        _uiState.update { it.copy(memoryDate = date, isPeriodMode = false) }
    }

    fun updateMemoryPeriod(start: Long?, end: Long?) {
        _uiState.update { it.copy(memoryDateStart = start, memoryDateEnd = end, isPeriodMode = true) }
    }
    
    fun togglePeriodMode(isPeriod: Boolean) {
        _uiState.update { it.copy(isPeriodMode = isPeriod) }
    }

    fun setLocation(locationId: String?) {
        if (locationId == null) {
            _uiState.update { it.copy(locationId = null, locationName = null) }
            return
        }
        
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId)
                    .collection("locations").document(locationId).get().await()
                
                if (doc.exists()) {
                    _uiState.update { it.copy(
                        locationId = locationId,
                        locationName = doc.getString("placeName") ?: "Lieu inconnu"
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("StepByStepVM", "Erreur résolution lieu", e)
            }
        }
    }

    // --- ENIGMA UPDATES (v9.4.27) ---
    fun updateEnigmaEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enigmaEnabled = enabled) }
    }
    fun updateEnigmaQuestion(q: String) {
        _uiState.update { it.copy(enigmaQuestion = q) }
    }
    fun updateEnigmaAnswer(a: String) {
        _uiState.update { it.copy(enigmaAnswer = a) }
    }
    fun updateEnigmaHint(h: String) {
        _uiState.update { it.copy(enigmaHint = h) }
    }
    fun updateAutoUnlockDays(days: Int?) {
        _uiState.update { it.copy(autoUnlockDays = days) }
    }
    fun updateUltimateSecret(ultimate: Boolean) {
        _uiState.update { it.copy(isUltimateSecret = ultimate) }
    }
}
