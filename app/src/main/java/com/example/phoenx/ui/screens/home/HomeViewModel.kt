package com.example.phoenx.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val aiManager: AIManager,
    private val protocolManager: ActivationProtocolManager,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _daysSincePresence = MutableStateFlow(0)
    val daysSincePresence: StateFlow<Int> = _daysSincePresence.asStateFlow()

    init {
        loadUserData()
        loadBiographerQuestion()
        observeLatestEntries()
        loadPendingQuestionsCount()
        loadExtraStats()
    }

    /**
     * Calcule le nombre de jours écoulés depuis la dernière présence confirmée.
     */
    fun calculateDaysSincePresence(lastCheckInTimestamp: Long) {
        val diff = System.currentTimeMillis() - lastCheckInTimestamp
        _daysSincePresence.value = (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun loadExtraStats() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // Nombre de questions répondues
                db.collection("users").document(user.uid)
                    .collection("entries")
                    .whereNotEqualTo("enigmaQuestion", null)
                    .addSnapshotListener { snapshot, _ ->
                        _uiState.update { it.copy(answeredQuestionsCount = snapshot?.size() ?: 0) }
                    }

                // Nombre de chapitres validés
                db.collection("users").document(user.uid)
                    .collection("book").document("default") // Hypothèse ID livre
                    .collection("chapters")
                    .whereEqualTo("status", "validated")
                    .addSnapshotListener { snapshot, _ ->
                        _uiState.update { it.copy(validatedChaptersCount = snapshot?.size() ?: 0) }
                    }
            } catch (e: Exception) {}
        }
    }

    private fun loadPendingQuestionsCount() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid)
                    .collection("pendingQuestions")
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshot, _ ->
                        _uiState.update { it.copy(pendingQuestionsCount = snapshot?.size() ?: 0) }
                    }
            } catch (e: Exception) {}
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("displayName") ?: user.email?.substringBefore("@") ?: "Ami"
                        val birthTimestamp = doc.getTimestamp("dateOfBirth")
                        val lastAlive = doc.getTimestamp("lastAliveConfirmedAt")
                        
                        var currentAge = 0
                        if (birthTimestamp != null) {
                            val birthDate = birthTimestamp.toDate()
                            val ageSnapshot = AgeUtils.calculateAge(birthDate)
                            currentAge = ageSnapshot.years
                        }

                        if (lastAlive != null) {
                            calculateDaysSincePresence(lastAlive.toDate().time)
                        }

                        _uiState.value = _uiState.value.copy(
                            userName = name,
                            userEmail = user.email ?: "",
                            currentAge = currentAge,
                            currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        )
                    }
            } catch (e: Exception) { }
        }
    }

    private fun observeLatestEntries() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { entries ->
                // Filtrer pour ne compter que les souvenirs racines (v8.3.4)
                val rootEntries = entries.filter { it.parentEntryId == null }
                
                val minAgeVal = if (rootEntries.isEmpty()) 0 else rootEntries.minOf { AgeUtils.parseAgeJson(it.ageAtCreation).years }
                
                _uiState.value = _uiState.value.copy(
                    latestEntries = rootEntries.take(5),
                    entryCount = rootEntries.size,
                    minAge = minAgeVal
                )
            }
        }
    }

    private fun loadBiographerQuestion() {
        viewModelScope.launch {
            try {
                val question = aiManager.getBiographerQuestion()
                _uiState.value = _uiState.value.copy(biographerQuestion = question)
            } catch (e: Exception) { }
        }
    }

    fun updateProofOfLife() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                protocolManager.confirmProofOfLife(userId)
                _daysSincePresence.value = 0
            } catch (e: Exception) { }
        }
    }
}

data class HomeUiState(
    val userName: String = "",
    val userEmail: String = "",
    val currentDate: String = "",
    val entryCount: Int = 0,
    val minAge: Int = 0,
    val currentAge: Int = 0,
    val biographerQuestion: String = "Quelle décision as-tu prise dont tu es le plus fier ?",
    val pendingQuestionsCount: Int = 0,
    val answeredQuestionsCount: Int = 0,
    val validatedChaptersCount: Int = 0,
    val latestEntries: List<OfflineEntry> = emptyList()
)
