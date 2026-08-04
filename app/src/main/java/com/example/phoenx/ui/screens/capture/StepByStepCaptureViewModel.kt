package com.example.phoenx.ui.screens.capture

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class StepByStepUiState(
    val currentStep: Int = 1,
    val title: String = "",
    val category: String = "Sagesse",
    val memoryDate: Long? = null,
    val memoryDateStart: Long? = null,
    val memoryDateEnd: Long? = null,
    val isPeriodMode: Boolean = false
)

@HiltViewModel
class StepByStepCaptureViewModel @Inject constructor() : ViewModel() {

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
}
