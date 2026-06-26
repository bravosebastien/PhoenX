package com.example.phoenx.ui.screens.recipient

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
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class RecipientMediaViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _libraryEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val libraryEntries: StateFlow<List<PhoenXEntry>> = _libraryEntries

    private val _discothequeEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val discothequeEntries: StateFlow<List<PhoenXEntry>> = _discothequeEntries

    private val _archiveEntries = MutableStateFlow<List<PhoenXEntry>>(emptyList())
    val archiveEntries: StateFlow<List<PhoenXEntry>> = _archiveEntries

    init {
        loadAllMedia()
    }

    private fun loadAllMedia() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { offlineEntries ->
                val decoded = offlineEntries.map { it.toDomain(encryptionManager) }
                
                _libraryEntries.value = decoded.filter { it.type == EntryType.THOUGHT || it.isYoungSelfLetter }
                _discothequeEntries.value = decoded.filter { it.type == EntryType.NIGHT_CAPTURE || it.type == EntryType.EMOTION } // Mapping to Audio
                _archiveEntries.value = decoded.filter { it.type == EntryType.LEGACY } // Mapping to Photos/Videos for now
            }
        }
    }

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager): PhoenXEntry {
        val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
        val decryptedText = encryptionManager.decryptText(encryptedPayload, tempKey)
        
        val ageJson = JSONObject(ageAtCreation)
        val age = AgeSnapshot(
            years = ageJson.getInt("years"),
            months = ageJson.getInt("months"),
            days = ageJson.getInt("days")
        )

        return PhoenXEntry(
            id = id,
            ageAtCreation = age,
            encryptedContent = decryptedText.toByteArray(),
            type = try { EntryType.valueOf(entryType) } catch(e: Exception) { EntryType.THOUGHT },
            isYoungSelfLetter = isYoungSelfLetter,
            targetAge = targetAge,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary
        )
    }
}
