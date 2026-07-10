package com.example.phoenx.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.FavoriteEntity
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            offlineEntryDao.getAllFavorites().collectLatest { entities ->
                val decoded = entities.map { it.toDomain() }
                
                // Simulation de génération de Carte des Goûts par l'IA
                val tasteMap = if (decoded.isNotEmpty()) {
                    "Ta bibliothèque révèle un attachement profond pour les récits d'aventure et la musique mélancolique. Tes choix tournent souvent autour du thème de la découverte."
                } else ""

                _uiState.value = FavoritesUiState.Success(decoded, tasteMap)
            }
        }
    }

    fun saveFavorite(category: String, title: String, why: String) {
        viewModelScope.launch {
            try {
                val encTitle = encryptionManager.encryptText(title)
                val encWhy = encryptionManager.encryptText(why)

                val entity = FavoriteEntity(
                    category = category,
                    encryptedTitle = encTitle,
                    encryptedWhy = encWhy
                )
                offlineEntryDao.insertFavorite(entity)
            } catch (_: Exception) {}
        }
    }

    private fun FavoriteEntity.toDomain(): FavoriteItem {
        return FavoriteItem(
            id = id,
            category = category,
            title = encryptionManager.decryptText(encryptedTitle),
            why = encryptionManager.decryptText(encryptedWhy)
        )
    }
}

data class FavoriteItem(
    val id: String,
    val category: String,
    val title: String,
    val why: String
)

sealed class FavoritesUiState {
    object Loading : FavoritesUiState()
    data class Success(
        val items: List<FavoriteItem>,
        val tasteMap: String = "" // Résumé IA des goûts
    ) : FavoritesUiState()
}
