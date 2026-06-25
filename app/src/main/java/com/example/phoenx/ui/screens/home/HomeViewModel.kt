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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
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

    init {
        loadUserData()
        loadBiographerQuestion()
        observeLatestEntries()
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

                        var daysSinceLastProof = 0
                        if (lastAlive != null) {
                            val diff = System.currentTimeMillis() - lastAlive.toDate().time
                            daysSinceLastProof = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                        }

                        _uiState.value = _uiState.value.copy(
                            userName = name,
                            currentAge = currentAge,
                            lastProofOfLifeDays = daysSinceLastProof,
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
                val minAgeVal = if (entries.isEmpty()) 0 else entries.minOf { AgeUtils.parseAgeJson(it.ageAtCreation).years }
                
                _uiState.value = _uiState.value.copy(
                    latestEntries = entries.take(5),
                    entryCount = entries.size,
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
                _uiState.value = _uiState.value.copy(lastProofOfLifeDays = 0)
            } catch (e: Exception) { }
        }
    }
}

data class HomeUiState(
    val userName: String = "",
    val currentDate: String = "",
    val entryCount: Int = 0,
    val minAge: Int = 0,
    val currentAge: Int = 0,
    val biographerQuestion: String = "Quelle décision as-tu prise dont tu es le plus fier ?",
    val lastProofOfLifeDays: Int = 0,
    val latestEntries: List<OfflineEntry> = emptyList()
)
