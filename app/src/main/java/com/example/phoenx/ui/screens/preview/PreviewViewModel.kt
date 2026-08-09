package com.example.phoenx.ui.screens.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.StandaloneMediaDao
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.StandaloneMediaEntity
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class PreviewDashboardState(
    val souvenirsCount: Int = 0,
    val photosCount: Int = 0,
    val videosCount: Int = 0,
    val audiosCount: Int = 0,
    val recipientName: String = "",
    val isLoading: Boolean = true,
    val filteredEntries: List<com.example.phoenx.data.local.OfflineEntry> = emptyList(),
    val filteredMedia: List<com.example.phoenx.domain.model.PhoenXEntry> = emptyList(),
    val filteredEnigmas: List<com.example.phoenx.data.local.OfflineEntry> = emptyList(),
    val familyCount: Int = 0,
    val bookTitle: String? = null,
    val hasBookDraft: Boolean = false
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: StandaloneMediaDao,
    private val encryptionManager: com.example.phoenx.data.encryption.EncryptionManager, // v9.4.27
    private val db: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(PreviewDashboardState())
    val state: StateFlow<PreviewDashboardState> = _state.asStateFlow()

    /**
     * Déchiffre le texte d'un souvenir pour l'aperçu (v9.4.27)
     */
    fun decryptContent(encryptedPayload: ByteArray): String {
        return encryptionManager.decryptText(encryptedPayload)
    }

    fun loadPreview(recipientUid: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // 1. Récupérer le nom du destinataire pour l'UI
            val recipients = offlineEntryDao.getAllRecipients().first()
            val recipient = recipients.find { it.linkedUid == recipientUid }
            val name = recipient?.name ?: "Ce proche"

            // 1b. Charger le titre du Livre et existence du draft (v9.4.27: Fix check)
            var bookTitle: String? = null
            var hasDraft = false
            try {
                val bookDoc = db.collection("users").document(userId)
                    .collection("book").document("current_draft").get().await()
                
                if (bookDoc.exists()) {
                    bookTitle = bookDoc.getString("bookTitle")
                    // On considère qu'il y a un draft si le document existe
                    hasDraft = true
                }
            } catch (e: Exception) {
                android.util.Log.e("PreviewVM", "Error loading book info", e)
            }

            // 2. Charger les contenus assignés (Réutilisation de la logique pure de filtrage)
            val entriesFlow = offlineEntryDao.getAllEntries()
            val standaloneFlow = standaloneMediaDao.getAllStandaloneMedia()
            val personsFlow = offlineEntryDao.getAllPersons()

            combine(entriesFlow, standaloneFlow, personsFlow) { entries, standalone, persons ->
                val filteredEntries = entries.filter { 
                    it.visibility == "EVERYONE" || it.recipientIds.split(",").map { id -> id.trim() }.contains(recipientUid) 
                }
                val filteredStandalone = standalone.filter { 
                    it.visibility == "EVERYONE" || it.recipientIds.split(",").map { id -> id.trim() }.contains(recipientUid) 
                }

                val souvenirs = filteredEntries.filter { it.parentEntryId == null && it.entryType != "PORTRAIT" }
                
                val allPhotosCount = filteredEntries.count { it.entryType == "PHOTO" } + 
                                   filteredStandalone.count { it.type == "PHOTO" }
                
                val allVideosCount = filteredEntries.count { it.entryType == "VIDEO" } + 
                                   filteredStandalone.count { it.type == "VIDEO" || it.type == "YOUTUBE" }
                
                val allAudiosCount = filteredEntries.count { it.entryType == "AUDIO" || it.entryType == "EMOTION" } + 
                                   filteredStandalone.count { it.type == "SPOTIFY" || it.type == "DEEZER" }

                val allMapped = filteredEntries.map { it.toSimpleDomain() } + filteredStandalone.map { it.toSimpleStandaloneDomain() }

                PreviewDashboardState(
                    souvenirsCount = souvenirs.size,
                    photosCount = allPhotosCount,
                    videosCount = allVideosCount,
                    audiosCount = allAudiosCount,
                    recipientName = name,
                    isLoading = false,
                    filteredEntries = souvenirs.sortedByDescending { it.createdAt },
                    filteredMedia = allMapped,
                    filteredEnigmas = filteredEntries.filter { it.enigmaQuestion != null },
                    familyCount = persons.size,
                    bookTitle = bookTitle,
                    hasBookDraft = hasDraft
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    private fun OfflineEntry.toSimpleDomain() = PhoenXEntry(
        id = id,
        aiSummary = aiSummary,
        type = when(entryType) {
            "PHOTO" -> EntryType.PHOTO
            "VIDEO" -> EntryType.VIDEO
            "AUDIO", "EMOTION" -> EntryType.AUDIO
            else -> EntryType.THOUGHT
        },
        parentEntryId = parentEntryId,
        mediaUrl = mediaUrl,
        localMediaPath = localMediaPath,
        coverUrl = coverUrl, // Crucial pour miniatures YouTube/Spotify
        localCoverPath = localCoverPath,
        mediaProvider = mediaProvider,
        userComment = userComment,
        ageAtCreation = AgeSnapshot(0, 0, 0),
        encryptedContent = ByteArray(0)
    )

    private fun StandaloneMediaEntity.toSimpleStandaloneDomain() = PhoenXEntry(
        id = id,
        aiSummary = title,
        type = when(type) {
            "PHOTO" -> EntryType.PHOTO
            "YOUTUBE", "VIDEO" -> EntryType.VIDEO
            "SPOTIFY", "DEEZER" -> EntryType.AUDIO
            else -> EntryType.THOUGHT
        },
        mediaUrl = if (type != "TEXT_EXCERPT") content else null,
        mediaProvider = mediaProvider ?: type,
        coverUrl = coverUrl, // Ajouté pour l'aperçu
        localCoverPath = localCoverPath, // Ajouté pour l'aperçu
        userComment = userComment,
        ageAtCreation = AgeSnapshot(0, 0, 0),
        encryptedContent = ByteArray(0)
    )
}
