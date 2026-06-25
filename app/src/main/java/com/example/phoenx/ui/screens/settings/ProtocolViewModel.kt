package com.example.phoenx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.DepositaryEntity
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtocolUiState())
    val uiState: StateFlow<ProtocolUiState> = _uiState

    init {
        loadDepositary()
    }

    private fun loadDepositary() {
        viewModelScope.launch {
            offlineEntryDao.getDepositary().collectLatest { dep ->
                if (dep != null) {
                    _uiState.value = _uiState.value.copy(
                        name = dep.name,
                        email = dep.email,
                        phone = dep.phone,
                        status = dep.status
                    )
                }
            }
        }
    }

    fun saveProtocol(name: String, email: String, phone: String, threshold: Int) {
        viewModelScope.launch {
            offlineEntryDao.clearDepositaries()
            val dep = DepositaryEntity(
                name = name,
                email = email,
                phone = phone
            )
            offlineEntryDao.insertDepositary(dep)
            // Update threshold logic here later
            _uiState.value = _uiState.value.copy(isSuccess = true)
        }
    }
}

data class ProtocolUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = "Dormant",
    val thresholdHours: Int = 72,
    val isSuccess: Boolean = false
)
