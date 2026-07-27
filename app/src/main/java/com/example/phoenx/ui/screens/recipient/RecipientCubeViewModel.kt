package com.example.phoenx.ui.screens.recipient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RecipientCubeViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipientCubeUiState>(RecipientCubeUiState.Loading)
    val uiState: StateFlow<RecipientCubeUiState> = _uiState.asStateFlow()

    fun loadCreatorInfo(creatorId: String) {
        viewModelScope.launch {
            _uiState.value = RecipientCubeUiState.Loading
            try {
                val data = hashMapOf("creatorId" to creatorId)
                val result = functions.getHttpsCallable("getCreatorBookStatus").call(data).await()
                val response = result.data as Map<*, *>
                
                val name = response["displayName"] as? String ?: "Ton proche"
                val isBookOpen = response["isBookOpen"] as? Boolean ?: false
                
                _uiState.value = RecipientCubeUiState.Success(
                    creatorName = name,
                    isActivated = isBookOpen
                )
            } catch (e: Exception) {
                android.util.Log.e("RecipientCube", "Erreur chargement infos : ${e.message}")
                _uiState.value = RecipientCubeUiState.Error(e.message ?: "Erreur de connexion")
            }
        }
    }
}

sealed class RecipientCubeUiState {
    object Loading : RecipientCubeUiState()
    data class Success(val creatorName: String, val isActivated: Boolean) : RecipientCubeUiState()
    data class Error(val message: String) : RecipientCubeUiState()
}
