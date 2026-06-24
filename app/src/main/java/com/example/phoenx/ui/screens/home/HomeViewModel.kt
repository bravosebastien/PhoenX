package com.example.phoenx.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val aiManager: AIManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadUserData()
        loadBiographerQuestion()
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("displayName") ?: user.email?.substringBefore("@") ?: "Ami"
                        val birthTimestamp = doc.getTimestamp("dateOfBirth")
                        
                        var currentAge = 0
                        if (birthTimestamp != null) {
                            val birthDate = birthTimestamp.toDate()
                            val ageSnapshot = AgeUtils.calculateAge(birthDate)
                            currentAge = ageSnapshot.years
                        }

                        _uiState.value = _uiState.value.copy(
                            userName = name,
                            currentAge = currentAge,
                            currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        )
                    }
            } catch (e: Exception) {
                // Gérer l'erreur
            }
        }
    }

    private fun loadBiographerQuestion() {
        viewModelScope.launch {
            try {
                val question = aiManager.getBiographerQuestion()
                _uiState.value = _uiState.value.copy(biographerQuestion = question)
            } catch (e: Exception) {
                // Garder la question par défaut si erreur (ex: pas de connexion)
            }
        }
    }

    fun updateProofOfLife() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).update("lastAliveConfirmedAt", Timestamp.now())
        _uiState.value = _uiState.value.copy(lastProofOfLifeDays = 0)
    }
}

data class HomeUiState(
    val userName: String = "",
    val currentDate: String = "",
    val entryCount: Int = 0,
    val minAge: Int = 0,
    val currentAge: Int = 0,
    val biographerQuestion: String = "Quelle décision as-tu prise dont tu es le plus fier ?",
    val lastProofOfLifeDays: Int = 0
)
