package com.example.phoenx.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class StepByStepUiState(
    val currentStep: Int = 1,
    val title: String = "",
    val category: String = "Sagesse",
    val memoryDate: Long? = null,
    val memoryDateStart: Long? = null,
    val memoryDateEnd: Long? = null,
    val isPeriodMode: Boolean = false,
    val locationId: String? = null,
    val locationName: String? = null,
    
    // v9.4.27 : Coffre-Fort
    val enigmaEnabled: Boolean = false,
    val enigmaQuestion: String = "",
    val enigmaAnswer: String = "",
    val enigmaHint: String = "",
    val autoUnlockDays: Int? = 30,
    val isUltimateSecret: Boolean = false,
    val includeInBook: Boolean = true, // v9.4.27
    val tonalNuance: String = "", // v9.4.27

    // v12.5 : Récit, destinataires et médias
    // RESTAURÉ le 18/09 — ces champs avaient disparu du modèle alors que les fonctions
    // qui les utilisent (plus bas) étaient restées en place, rendant le fichier incohérent.
    val story: String = "",
    val visibility: String = "RESTRICTED",
    val selectedRecipientIds: List<String> = emptyList(),
    val mediaAttachments: List<Pair<java.io.File, String>> = emptyList(), // (Fichier, Type: PHOTO|VIDEO|AUDIO)

    // v12.6 : Liens externes (Spotify/Deezer/YouTube) en attente d'être créés à la sauvegarde finale
    val pendingLinks: List<PendingLink> = emptyList()
)

// v12.6 : Un lien externe saisi pendant la création, avant que le souvenir n'existe encore
data class PendingLink(
    val provider: String, // "SPOTIFY", "DEEZER", "YOUTUBE"
    val title: String,
    val comment: String?,
    val url: String,
    val thumbnailUrl: String?
)

@HiltViewModel
class StepByStepCaptureViewModel @Inject constructor(
    private val db: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val offlineEntryDao: com.example.phoenx.data.local.OfflineEntryDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepByStepUiState())
    val uiState: StateFlow<StepByStepUiState> = _uiState.asStateFlow()

    private val _recipients = MutableStateFlow<List<com.example.phoenx.data.local.RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = _recipients.asStateFlow()

    init {
        loadRecipients()
    }

    private fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collect { list ->
                _recipients.value = list
            }
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = minOf(it.currentStep + 1, 8)) }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun updateMemoryDate(date: Long?) {
        _uiState.update { it.copy(memoryDate = date, isPeriodMode = false) }
    }

    fun updateMemoryPeriod(start: Long?, end: Long?) {
        _uiState.update { it.copy(memoryDateStart = start, memoryDateEnd = end, isPeriodMode = true) }
    }
    
    fun togglePeriodMode(isPeriod: Boolean) {
        _uiState.update { it.copy(isPeriodMode = isPeriod) }
    }

    fun setLocation(locationId: String?) {
        if (locationId == null) {
            _uiState.update { it.copy(locationId = null, locationName = null) }
            return
        }
        
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId)
                    .collection("locations").document(locationId).get().await()
                
                if (doc.exists()) {
                    _uiState.update { it.copy(
                        locationId = locationId,
                        locationName = doc.getString("placeName") ?: context.getString(R.string.step_capture_viewmodel_location_unknown)
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("StepByStepVM", "Erreur résolution lieu", e)
            }
        }
    }

    /**
     * v13.0 : Géolocalisation directe — crée un nouveau lieu à partir de la position GPS
     * actuelle du téléphone (alternative à la sélection manuelle sur la Mappemonde),
     * puis l'attache immédiatement au souvenir en cours de création. Réutilise le même
     * schéma Firestore que MappamondeViewModel.pinLocation.
     */
    fun useCurrentGpsLocation(latitude: Double, longitude: Double, placeName: String, countryName: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val newLoc = com.example.phoenx.ui.screens.mappemonde.LocationMemory(
                    latitude = latitude,
                    longitude = longitude,
                    placeName = placeName,
                    countryName = countryName,
                    visitedAt = System.currentTimeMillis()
                )
                val ref = db.collection("users").document(userId)
                    .collection("locations").add(newLoc).await()
                _uiState.update { it.copy(locationId = ref.id, locationName = placeName) }
            } catch (e: Exception) {
                android.util.Log.e("StepByStepVM", "Erreur géolocalisation", e)
            }
        }
    }

    // --- ENIGMA UPDATES (v9.4.27) ---
    fun updateEnigmaEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enigmaEnabled = enabled) }
    }
    fun updateEnigmaQuestion(q: String) {
        _uiState.update { it.copy(enigmaQuestion = q) }
    }
    fun updateEnigmaAnswer(a: String) {
        _uiState.update { it.copy(enigmaAnswer = a) }
    }
    fun updateEnigmaHint(h: String) {
        _uiState.update { it.copy(enigmaHint = h) }
    }
    fun updateAutoUnlockDays(days: Int?) {
        _uiState.update { it.copy(autoUnlockDays = days) }
    }
    fun updateUltimateSecret(ultimate: Boolean) {
        _uiState.update { it.copy(isUltimateSecret = ultimate) }
    }

    fun updateIncludeInBook(include: Boolean) {
        _uiState.update { it.copy(includeInBook = include) }
    }

    fun updateTonalNuance(nuance: String) {
        _uiState.update { it.copy(tonalNuance = nuance) }
    }

    // --- DESTINATAIRES & MÉDIAS (v12.5) ---
    fun toggleRecipient(id: String) {
        _uiState.update { state ->
            val current = state.selectedRecipientIds
            val newList = if (current.contains(id)) current.filter { it != id } else current + id
            state.copy(selectedRecipientIds = newList.distinct())
        }
    }

    fun updateVisibility(visibility: String) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    fun addMediaAttachment(file: java.io.File, type: String) {
        _uiState.update { it.copy(mediaAttachments = it.mediaAttachments + (file to type)) }
    }

    fun removeMediaAttachment(index: Int) {
        _uiState.update { it.copy(mediaAttachments = it.mediaAttachments.toMutableList().apply { removeAt(index) }) }
    }

    fun updateStory(story: String) {
        _uiState.update { it.copy(story = story) }
    }

    // v12.6 : Liens externes (Spotify/Deezer/YouTube)
    fun addPendingLink(link: PendingLink) {
        _uiState.update { it.copy(pendingLinks = it.pendingLinks + link) }
    }

    fun removePendingLink(index: Int) {
        _uiState.update { it.copy(pendingLinks = it.pendingLinks.toMutableList().apply { removeAt(index) }) }
    }
}
