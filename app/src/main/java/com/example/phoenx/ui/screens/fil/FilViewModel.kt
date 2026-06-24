package com.example.phoenx.ui.screens.fil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import org.json.JSONObject

@HiltViewModel
class FilViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FilUiState>(FilUiState(isLoading = true))
    val uiState: StateFlow<FilUiState> = _uiState

    init {
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            // On observe la base de données locale en temps réel
            offlineEntryDao.getAllEntries().collectLatest { offlineEntries ->
                val decodedEntries = offlineEntries.map { it.toDomain(encryptionManager) }
                
                _uiState.value = FilUiState(
                    entries = decodedEntries.sortedByDescending { it.timestamp },
                    totalCount = decodedEntries.size,
                    minAge = decodedEntries.minOfOrNull { it.ageAtCreation.years } ?: 0,
                    maxAge = decodedEntries.maxOfOrNull { it.ageAtCreation.years } ?: 0,
                    isLoading = false
                )
            }
        }
    }

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager): PhoenXEntry {
        // 1. Déchiffrer le contenu (on utilise la clé temporaire du MVP)
        val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
        val decryptedText = encryptionManager.decryptText(encryptedPayload, tempKey)
        
        // 2. Parser l'âge stocké en JSON
        val ageJson = JSONObject(ageAtCreation)
        val age = AgeSnapshot(
            years = ageJson.getInt("years"),
            months = ageJson.getInt("months"),
            days = ageJson.getInt("days")
        )

        return PhoenXEntry(
            id = id,
            ageAtCreation = age,
            encryptedContent = decryptedText.toByteArray(), // On garde le format ByteArray pour le modèle
            type = try { EntryType.valueOf(entryType) } catch(e: Exception) { EntryType.THOUGHT },
            timestamp = Instant.ofEpochMilli(createdAt)
        )
    }
}

data class FilUiState(
    val entries: List<PhoenXEntry> = emptyList(),
    val totalCount: Int = 0,
    val minAge: Int = 0,
    val maxAge: Int = 0,
    val isLoading: Boolean = false
)
