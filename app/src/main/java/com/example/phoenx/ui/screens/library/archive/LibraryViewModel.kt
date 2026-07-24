package com.example.phoenx.ui.screens.library.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.ui.screens.library.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

data class OldLibraryUiState(
    val compartments: List<LibraryCompartment> = emptyList(),
    val viewerMode: ViewerMode = ViewerMode.RECIPIENT_FULL,
    val recipientName: String = "",
    val creatorName: String = "",
    val isLoading: Boolean = true,
    val selectedCompartment: LibraryCompartment? = null,
    val glowIntensity: Float = 1f,
)

@HiltViewModel
class OldLibraryViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(OldLibraryUiState())
    val uiState: StateFlow<OldLibraryUiState> = _uiState.asStateFlow()

    fun initialize(
        creatorId: String,
        recipientId: String?,
        viewerMode: ViewerMode
    ) {
        viewModelScope.launch {
            try {
                // Pour le moment on simule les droits d'accès
                val accessRights = CompartmentId.entries.associateWith { CompartmentAccess.OPEN }

                // Récupérer les comptes d'items par compartiment via Room (v8.9.9 : Thread Off-UI)
                val entries = withContext(Dispatchers.IO) {
                    offlineEntryDao.getAllEntriesSync()
                }

                val itemCounts = mutableMapOf<CompartmentId, Int>()
                
                entries.forEach { entry ->
                    val compId = when(entry.entryType) {
                        "TEXT" -> CompartmentId.BIBLIOTHEQUE
                        "AUDIO", "NIGHT" -> CompartmentId.DISCOTHEQUE
                        "PHOTO", "VIDEO" -> CompartmentId.VIDEOTHEQUE
                        "LOCATION" -> CompartmentId.MAPPEMONDE
                        "QUESTION_ANSWER" -> CompartmentId.CENT_QUESTIONS
                        else -> CompartmentId.BIBLIOTHEQUE
                    }
                    itemCounts[compId] = (itemCounts[compId] ?: 0) + 1
                }

                // Construire la liste des compartiments
                val compartments = buildCompartments(accessRights, itemCounts, viewerMode)

                // Utilisation de auth et firestore pour récupérer le nom réel si disponible
                val creatorName = try {
                    firestore.collection("users").document(creatorId).get().await().getString("displayName") ?: "Créateur"
                } catch (_: Exception) { "Moi" }

                val recipientName = if (recipientId != null) {
                    try {
                        firestore.collection("users").document(creatorId).collection("recipients").document(recipientId).get().await().getString("name") ?: ""
                    } catch (_: Exception) { "" }
                } else ""

                _uiState.update {
                    it.copy(
                        compartments = compartments,
                        viewerMode = viewerMode,
                        creatorName = creatorName,
                        recipientName = recipientName,
                        isLoading = false
                    )
                }

                startGlowAnimation()

            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun buildCompartments(
        accessRights: Map<CompartmentId, CompartmentAccess>,
        itemCounts: Map<CompartmentId, Int>,
        viewerMode: ViewerMode
    ): List<LibraryCompartment> {
        return CompartmentId.entries.map { id ->
            LibraryCompartment(
                id = id,
                title = id.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                subtitle = "Découvrir",
                access = accessRights[id] ?: CompartmentAccess.OPEN,
                itemCount = itemCounts[id] ?: 0,
                route = when(id) {
                    CompartmentId.BIBLIOTHEQUE -> "recipient/library"
                    CompartmentId.DISCOTHEQUE -> "recipient/discotheque"
                    CompartmentId.VIDEOTHEQUE -> "recipient/videotheque"
                    CompartmentId.MAPPEMONDE -> "map"
                    CompartmentId.CENT_QUESTIONS -> "questions_room"
                    CompartmentId.COFFRE_FORT -> "recipient/detective"
                    else -> "home"
                }
            )
        }.filter { (it.itemCount > 0) || (viewerMode == ViewerMode.CREATOR_PREVIEW) }
    }

    private fun startGlowAnimation() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000L)
                _uiState.update { it.copy(glowIntensity = 0.8f) }
                kotlinx.coroutines.delay(2000L)
                _uiState.update { it.copy(glowIntensity = 1f) }
            }
        }
    }

    fun onCompartmentSelected(compartment: LibraryCompartment) {
        _uiState.update { it.copy(selectedCompartment = compartment) }
    }

    fun onDismissCompartment() {
        _uiState.update { it.copy(selectedCompartment = null) }
    }
}
