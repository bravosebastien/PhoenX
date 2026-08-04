package com.example.phoenx.ui.screens.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.domain.model.SimplifiedPerson
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
            offlineEntryDao.getAllRecipients(),
            offlineEntryDao.getAllWitnesses(),
            offlineEntryDao.getAllDepositaries(),
            offlineEntryDao.getAllEntries()
        ) { persons, recipients, witnesses, depositaries, entries ->
            // Unification des sources (v9.4.26)
            val allSimplified = mutableListOf<SimplifiedPerson>()

            // 1. Arbre & Livre (Priorité)
            persons.forEach {
                allSimplified.add(SimplifiedPerson(
                    id = it.id,
                    name = it.firstName + (it.lastName?.let { l -> " $l" } ?: ""),
                    photoUrl = it.imagePath,
                    sourceType = "arbre_livre",
                    relationship = it.relationship
                ))
            }

            // 2. Destinataires
            recipients.forEach {
                allSimplified.add(SimplifiedPerson(
                    id = it.id,
                    name = it.name,
                    photoUrl = it.photoUrl,
                    sourceType = "destinataire",
                    relationship = it.relationship
                ))
            }

            // 3. Témoins
            witnesses.forEach {
                allSimplified.add(SimplifiedPerson(
                    id = it.id,
                    name = it.name,
                    photoUrl = it.photoUrl,
                    sourceType = "temoin"
                ))
            }

            // 4. Dépositaires
            depositaries.forEach {
                allSimplified.add(SimplifiedPerson(
                    id = it.id,
                    name = it.name,
                    photoUrl = it.photoUrl,
                    sourceType = "depositaire",
                    relationship = if (it.role == "primary") "Gardien Principal" else "Gardien Secondaire"
                ))
            }

            // Déduplication par nom (Insensible à la casse)
            val uniquePersons = allSimplified.distinctBy { it.name.lowercase().trim() }

            val characters = uniquePersons.map { person ->
                // On compte les apparitions dans les souvenirs
                // Note : on cherche par ID ou par nom si l'ID a changé suite à la déduplication
                // Pour l'instant on garde l'ID original de l'entrée dédupliquée choisie
                val appearanceCount = entries.count { entry ->
                    val ids = entry.personIds.split(",").filter { it.isNotBlank() }.map { it.trim() }
                    ids.contains(person.id)
                }
                CharacterWithStats(person, appearanceCount)
            }
            CharactersUiState.Success(characters.sortedByDescending { it.appearanceCount })
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
    val person: SimplifiedPerson,
    val appearanceCount: Int
)
