package com.example.phoenx.ui.screens.youngselfletters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class YoungSelfLetterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val aiManager: AIManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(YoungSelfLetterUiState())
    val uiState: StateFlow<YoungSelfLetterUiState> = _uiState

    init {
        loadUserAge()
    }

    private fun loadUserAge() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                _uiState.value = _uiState.value.copy(currentAge = age.years)
            } catch (e: Exception) {
                // Fallback age
                _uiState.value = _uiState.value.copy(currentAge = 40)
            }
        }
    }

    /**
     * Suggère des thèmes basés sur les pensées de l'utilisateur à l'âge cible.
     */
    fun getSuggestions(targetAge: Int) {
        viewModelScope.launch {
            // Dans le MVP 5.0, on simule l'appel cloud pour les suggestions
            // car on n'a pas encore toutes les entrées filtrées par âge exact en local
            _uiState.value = _uiState.value.copy(isLoadingSuggestions = true)
            
            // Simulation d'une recherche locale de résumés à cet âge
            val summaries = listOf("Incertitude professionnelle", "Premiers amours", "Voyage en sac à dos") 
            
            // Note: En prod, on appellerait aiManager.generateYoungSelfSuggestions(targetAge, summaries)
            val suggestions = "L'IA se souvient qu'à cet âge, tu parlais souvent d'incertitude et de liberté. Tu pourrais lui dire ce que tu as appris sur ces deux thèmes."
            
            _uiState.value = _uiState.value.copy(
                aiSuggestions = suggestions,
                isLoadingSuggestions = false
            )
        }
    }
}

data class YoungSelfLetterUiState(
    val currentAge: Int = 0,
    val aiSuggestions: String? = null,
    val isLoadingSuggestions: Boolean = false
)
