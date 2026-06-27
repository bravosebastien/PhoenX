package com.example.phoenx.ui.screens.questionsroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class QuestionsRoomViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val aiManager: AIManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionsRoomUiState())
    val uiState: StateFlow<QuestionsRoomUiState> = _uiState

    fun askQuestion(query: String) {
        if (query.isBlank()) return
        
        _uiState.value = _uiState.value.copy(isSearching = true, currentQuery = query)
        
        viewModelScope.launch {
            try {
                // 1. Récupérer tous les résumés IA (non chiffrés) de la base locale
                val allEntries = offlineEntryDao.getAllEntriesSync()
                val summariesWithIds = allEntries.map { it.id to it.aiSummary }
                
                // 2. Envoyer la question et les résumés à l'IA Cloud (Vertex AI)
                // Note: En prod, on utiliserait une Cloud Function dédiée.
                // Ici on simule la sélection sémantique par l'IA.
                val relevantIds = performSimulatedSemanticSearch(query, summariesWithIds)
                
                // 3. Récupérer et déchiffrer les entrées correspondantes
                val results = allEntries.filter { relevantIds.contains(it.id) }
                    .map { it.toDomain(encryptionManager) }
                
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    results = results,
                    hasSearched = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false, error = "L'IA n'a pas pu traiter votre demande.")
            }
        }
    }

    private fun performSimulatedSemanticSearch(query: String, summaries: List<Pair<String, String>>): List<String> {
        // Simulation : on cherche les mots clés dans les résumés
        // En version finale, c'est Gemini 3.1 Flash qui fait ce tri intelligemment
        val keywords = query.lowercase().split(" ")
        return summaries.filter { (_, summary) ->
            keywords.any { summary.lowercase().contains(it) }
        }.take(3).map { it.first }
    }

    private fun OfflineEntry.toDomain(encryptionManager: EncryptionManager): PhoenXEntry {
        val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
        val decryptedText = try { 
            encryptionManager.decryptText(encryptedPayload, tempKey) 
        } catch(e: Exception) { "Contenu protégé" }
        
        val ageJson = JSONObject(ageAtCreation)
        return PhoenXEntry(
            id = id,
            ageAtCreation = AgeSnapshot(ageJson.getInt("years"), ageJson.getInt("months"), ageJson.getInt("days")),
            encryptedContent = decryptedText.toByteArray(),
            type = EntryType.THOUGHT,
            timestamp = Instant.ofEpochMilli(createdAt),
            aiSummary = aiSummary
        )
    }
}

data class QuestionsRoomUiState(
    val currentQuery: String = "",
    val results: List<PhoenXEntry> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)
