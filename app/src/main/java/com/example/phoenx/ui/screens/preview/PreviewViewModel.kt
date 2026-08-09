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
    val filteredSouvenirs: List<OfflineEntry> = emptyList(),
    val allFilteredEntries: List<OfflineEntry> = emptyList(),
    val filteredMedia: List<PhoenXEntry> = emptyList(),
    val filteredEnigmas: List<OfflineEntry> = emptyList(),
    val familyCount: Int = 0,
    val bookTitle: String? = null,
    val hasBookDraft: Boolean = false,
    val isBookShared: Boolean = false // v9.4.27
)

data class BookPreviewInfo(
    val title: String? = null,
    val hasDraft: Boolean = false,
    val isShared: Boolean = false // v9.4.27
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val standaloneMediaDao: StandaloneMediaDao,
    private val encryptionManager: com.example.phoenx.data.encryption.EncryptionManager,
    private val db: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _recipientUid = MutableStateFlow<String?>(null)
    private val _bookInfo = MutableStateFlow(BookPreviewInfo())

    // Source de vérité unique pour l'UI (v9.4.27)
    val state: StateFlow<PreviewDashboardState> = _recipientUid
        .filterNotNull()
        .flatMapLatest { uid ->
            val entriesFlow = offlineEntryDao.getAllEntries()
            val standaloneFlow = standaloneMediaDao.getAllStandaloneMedia()
            val personsFlow = offlineEntryDao.getAllPersons()
            
            // Chargement asynchrone du nom du destinataire
            val nameFlow = flow {
                val recipients = offlineEntryDao.getAllRecipients().first()
                emit(recipients.find { it.linkedUid == uid }?.name ?: "Ce proche")
            }

            combine(entriesFlow, standaloneFlow, personsFlow, _bookInfo, nameFlow) { entries, standalone, persons, bookInfo, name ->
                val filteredEntries = entries.filter { 
                    it.visibility == "EVERYONE" || it.recipientIds.split(",").map { id -> id.trim() }.contains(uid) 
                }
                val filteredStandalone = standalone.filter { 
                    it.visibility == "EVERYONE" || it.recipientIds.split(",").map { id -> id.trim() }.contains(uid) 
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
                    filteredSouvenirs = souvenirs.sortedByDescending { it.createdAt },
                    allFilteredEntries = filteredEntries,
                    filteredMedia = allMapped,
                    filteredEnigmas = filteredEntries.filter { it.enigmaQuestion != null },
                    familyCount = persons.size,
                    bookTitle = bookInfo.title,
                    hasBookDraft = bookInfo.hasDraft,
                    isBookShared = bookInfo.isShared
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreviewDashboardState())

    /**
     * Déchiffre le texte d'un souvenir pour l'aperçu (v9.4.27)
     */
    fun decryptContent(encryptedPayload: ByteArray, summary: String): String {
        android.util.Log.d("PreviewVM_Debug", "--- DIAGNOSTIC SOUVENIR ---")
        android.util.Log.d("PreviewVM_Debug", "Titre (aiSummary): $summary")
        android.util.Log.d("PreviewVM_Debug", "Taille brute payload: ${encryptedPayload.size} bytes")
        
        val decrypted = try {
            encryptionManager.decryptText(encryptedPayload)
        } catch (e: Exception) {
            "Erreur déchiffrement"
        }

        android.util.Log.d("PreviewVM_Debug", "Texte déchiffré: $decrypted")
        android.util.Log.d("PreviewVM_Debug", "Identiques ? ${decrypted.trim() == summary.trim()}")
        
        return decrypted
    }

    fun loadPreview(recipientUid: String) {
        if (_recipientUid.value == recipientUid) return // Évite de recharger si déjà sur le même UID
        
        val userId = auth.currentUser?.uid ?: return
        _recipientUid.value = recipientUid
        
        // Un seul lancement de chargement Cloud
        viewModelScope.launch {
            try {
                android.util.Log.d("PreviewVM_Debug", "Fetching book info for creator: $userId")
                val bookDoc = db.collection("users").document(userId)
                    .collection("book").document("current_draft").get().await()
                
                if (bookDoc.exists()) {
                    val bookTitle = bookDoc.getString("bookTitle")
                    val chapters = bookDoc.get("chapters") as? List<*>
                    val recIds = (bookDoc.get("recipientIds") as? List<*>)?.map { it.toString() } ?: emptyList()
                    val visibility = bookDoc.getString("visibility") ?: "RESTRICTED"
                    
                    val hasDraft = chapters?.isNotEmpty() == true
                    val isVisible = visibility == "EVERYONE" || recIds.contains(recipientUid)
                    
                    _bookInfo.value = BookPreviewInfo(bookTitle, hasDraft, isVisible)
                    android.util.Log.d("PreviewVM_Debug", "Book Info updated: hasDraft=$hasDraft, isVisible=$isVisible")
                } else {
                    _bookInfo.value = BookPreviewInfo(null, false, false)
                }
            } catch (e: Exception) {
                android.util.Log.e("PreviewVM_Debug", "Error loading book info", e)
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
