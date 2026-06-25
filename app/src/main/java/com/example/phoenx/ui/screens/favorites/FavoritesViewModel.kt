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
                _uiState.value = FavoritesUiState.Success(decoded)
            }
        }
    }

    fun saveFavorite(category: String, title: String, why: String) {
        viewModelScope.launch {
            try {
                val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
                val encTitle = encryptionManager.encryptText(title, tempKey)
                val encWhy = encryptionManager.encryptText(why, tempKey)

                val entity = FavoriteEntity(
                    category = category,
                    encryptedTitle = encTitle,
                    encryptedWhy = encWhy
                )
                offlineEntryDao.insertFavorite(entity)
            } catch (e: Exception) {}
        }
    }

    private fun FavoriteEntity.toDomain(): FavoriteItem {
        val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
        return FavoriteItem(
            id = id,
            category = category,
            title = encryptionManager.decryptText(encryptedTitle, tempKey),
            why = encryptionManager.decryptText(encryptedWhy, tempKey)
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
    data class Success(val items: List<FavoriteItem>) : FavoritesUiState()
}
