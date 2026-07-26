package com.example.phoenx.ui.screens.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<CharactersUiState>(CharactersUiState.Loading)
    val uiState: StateFlow<CharactersUiState> = _uiState.asStateFlow()

    init {
        loadCharacters()
    }

    private fun loadCharacters() {
        combine(
            offlineEntryDao.getAllPersons(),
            offlineEntryDao.getAllEntries()
        ) { persons, entries ->
            val characters = persons.map { person ->
                val appearanceCount = entries.count { entry ->
                    entry.personIds.split(",").contains(person.id)
                }
                CharacterWithStats(person, appearanceCount)
            }
            CharactersUiState.Success(characters)
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }
}

sealed class CharactersUiState {
    object Loading : CharactersUiState()
    data class Success(val characters: List<CharacterWithStats>) : CharactersUiState()
}

data class CharacterWithStats(
    val person: PersonEntity,
    val appearanceCount: Int
)
