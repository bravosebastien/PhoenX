package com.example.phoenx.ui.screens.worlds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorldsViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorldsUiState>(WorldsUiState.Loading)
    val uiState: StateFlow<WorldsUiState> = _uiState

    init {
        loadWorlds()
    }

    private fun loadWorlds() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { entries ->
                if (entries.isEmpty()) {
                    _uiState.value = WorldsUiState.Empty
                } else {
                    // Groupement automatique par catégorie émotionnelle (Rangement IA)
                    val grouped = entries.groupBy { it.emotionalCategory }
                    val worlds = grouped.map { (cat, items) ->
                        WorldItem(
                            name = cat,
                            count = items.size,
                            lastEntrySummary = items.firstOrNull()?.aiSummary ?: ""
                        )
                    }.sortedByDescending { it.count }
                    
                    _uiState.value = WorldsUiState.Success(worlds)
                }
            }
        }
    }
}

data class WorldItem(
    val name: String,
    val count: Int,
    val lastEntrySummary: String
)

sealed class WorldsUiState {
    object Loading : WorldsUiState()
    object Empty : WorldsUiState()
    data class Success(val worlds: List<WorldItem>) : WorldsUiState()
}
