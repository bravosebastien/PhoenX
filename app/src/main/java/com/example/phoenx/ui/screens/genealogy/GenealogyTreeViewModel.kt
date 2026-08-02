package com.example.phoenx.ui.screens.genealogy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PersonEntity
import com.example.phoenx.data.local.PersonMediaDao
import com.example.phoenx.data.local.PersonMediaEntity
import com.example.phoenx.domain.genealogy.TreeAlgorithm
import com.example.phoenx.domain.model.TreeLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenealogyTreeViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val personMediaDao: PersonMediaDao
) : ViewModel() {

    val allPersons: StateFlow<List<PersonEntity>> = offlineEntryDao.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Calcul du layout pour le rendu visuel (v9.4.22)
     */
    val treeLayout: StateFlow<TreeLayout> = allPersons.map { persons ->
        val resolved = persons.map { it.toResolvedPerson(it.imagePath) }
        TreeAlgorithm.calculateLayout(resolved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TreeLayout(emptyList(), emptyList()))

    /**
     * Reconstruit la hiérarchie en mémoire (v9.4.22)
     * On cherche les "racines" (ceux qui n'ont pas de parents enregistrés)
     */
    val rootPersons: StateFlow<List<PersonEntity>> = allPersons.map { list ->
        list.filter { it.parentIds.isBlank() || it.parentIds == ",," }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Récupère les enfants d'une personne donnée (Reverse Lookup)
     */
    fun getChildrenOf(personId: String): Flow<List<PersonEntity>> {
        return offlineEntryDao.getChildrenOf(personId)
    }

    /**
     * Ajoute un lien de parenté (v9.4.22)
     * @param parentId L'ID du parent à ajouter
     * @param childId L'ID de la personne qui reçoit ce parent
     */
    fun linkParent(parentId: String, childId: String) {
        viewModelScope.launch {
            val child = allPersons.value.find { it.id == childId } ?: return@launch
            val currentParents = child.parentIds.trim(',').split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            
            if (!currentParents.contains(parentId) && currentParents.size < 2) {
                currentParents.add(parentId)
                val newCsv = "," + currentParents.joinToString(",") + ","
                val updatedChild = child.copy(parentIds = newCsv, syncStatus = "pending")
                offlineEntryDao.insertPerson(updatedChild)
            }
        }
    }

    fun updateBiography(personId: String, bio: String) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val updated = person.copy(biography = bio, syncStatus = "pending")
            offlineEntryDao.insertPerson(updated)
        }
    }

    fun toggleDeceased(personId: String) {
        viewModelScope.launch {
            val person = allPersons.value.find { it.id == personId } ?: return@launch
            val updated = person.copy(isDeceased = !person.isDeceased, syncStatus = "pending")
            offlineEntryDao.insertPerson(updated)
        }
    }

    // --- GESTION MÉDIAS ---

    fun getMediaForPerson(personId: String): Flow<List<PersonMediaEntity>> {
        return personMediaDao.getMediaForPerson(personId)
    }

    fun addMedia(personId: String, path: String, type: String) {
        viewModelScope.launch {
            val media = PersonMediaEntity(
                personId = personId,
                mediaPath = path,
                mediaType = type,
                syncStatus = "pending"
            )
            personMediaDao.insertMedia(media)
        }
    }

    fun removeMedia(media: PersonMediaEntity) {
        viewModelScope.launch {
            personMediaDao.deleteMedia(media)
        }
    }
}
