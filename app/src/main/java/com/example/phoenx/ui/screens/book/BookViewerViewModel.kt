package com.example.phoenx.ui.screens.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.model.BookDraft
import com.example.phoenx.service.BookGeneratorService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await as kotlinAwait
import javax.inject.Inject

/**
 * BookViewerViewModel — Gère l'affichage du livre scellé et illustré.
 */
@HiltViewModel
class BookViewerViewModel @Inject constructor(
    private val bookService: BookGeneratorService,
    private val auth: FirebaseAuth,
    private val functions: com.google.firebase.functions.FirebaseFunctions,
    private val encryptionManager: EncryptionManager,
    private val offlineEntryDao: OfflineEntryDao,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft.asStateFlow()

    private val _decryptedChapters = MutableStateFlow<Map<String, String>>(emptyMap())
    val decryptedChapters: StateFlow<Map<String, String>> = _decryptedChapters.asStateFlow()

    private val _mediaMap = MutableStateFlow<Map<String, OfflineEntry>>(emptyMap())
    val mediaMap: StateFlow<Map<String, OfflineEntry>> = _mediaMap.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _sealedMessage = MutableStateFlow<String?>(null)
    val sealedMessage: StateFlow<String?> = _sealedMessage.asStateFlow()

    private val _creatorName = MutableStateFlow("Ton proche")
    val creatorName: StateFlow<String> = _creatorName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadBook(targetCreatorId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = targetCreatorId ?: auth.currentUser?.uid
                if (userId == null) return@launch

                // 1. Vérification de sécurité via Cloud Function (si c'est un proche)
                if (targetCreatorId != null) {
                    try {
                        val result = functions.getHttpsCallable("getCreatorBookStatus")
                            .call(mapOf("creatorId" to targetCreatorId))
                            .kotlinAwait()
                        
                        val data = result.data as Map<*, *>
                        _creatorName.value = data["displayName"] as? String ?: "Ton proche"
                        val isBookOpen = data["isBookOpen"] as? Boolean ?: false
                        _sealedMessage.value = data["sealedMessage"] as? String
                        
                        if (!isBookOpen) {
                            _isLocked.value = true
                            _isLoading.value = false
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PHOENX_BOOK", "Accès refusé ou erreur statut", e)
                        _isLocked.value = true
                        _isLoading.value = false
                        return@launch
                    }
                }

                _isLocked.value = false
                val draft = bookService.loadBookDraft(userId)
                _bookDraft.value = draft

                if (draft != null) {
                    decryptAndResolveMedia(userId, draft)
                }

            } catch (e: Exception) {
                android.util.Log.e("PHOENX_BOOK", "Erreur chargement livre: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun decryptAndResolveMedia(userId: String, draft: BookDraft) {
        val chapterContents = mutableMapOf<String, String>()
        val mediaIds = mutableSetOf<String>()

        // 1. RÉCUPÉRATION DE LA CLÉ DU LIVRE
        var bookKey: ByteArray? = null
        try {
            val keyDoc = db.collection("users").document(userId)
                .collection("book_keys").document("main").get().kotlinAwait()
            val keyBase64 = keyDoc.getString("key")
            if (keyBase64 != null) {
                bookKey = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            android.util.Log.e("PHOENX_BOOK", "Impossible de récupérer la clé du livre", e)
        }

        // 2. Déchiffrement des chapitres
        draft.chapters.forEach { chapter ->
            val decrypted = encryptionManager.decrypt(chapter.content, bookKey)
            chapterContents[chapter.id] = decrypted
            
            // Extraction des IDs média [PHOTO:uuid] ou [AUDIO:uuid]
            val regex = Regex("\\[(PHOTO|AUDIO):([a-f0-9\\-]+)\\]")
            regex.findAll(decrypted).forEach { match ->
                mediaIds.add(match.groupValues[2])
            }
        }
        _decryptedChapters.value = chapterContents

        // 3. Résolution des médias
        val resolvedMedia = mutableMapOf<String, OfflineEntry>()
        mediaIds.forEach { mediaId ->
            try {
                // Tentative 1 : Local (Room) - Priorité Créateur
                var entry = offlineEntryDao.getEntryById(mediaId).firstOrNull()
                
                // Tentative 2 : Firestore (Si héritier ou si local absent)
                if (entry == null) {
                    val doc = db.collection("users").document(userId)
                        .collection("entries").document(mediaId).get().kotlinAwait()
                    if (doc.exists()) {
                        // On crée une OfflineEntry factice pour le transport des URLs
                        entry = OfflineEntry(
                            id = mediaId,
                            encryptedPayload = byteArrayOf(),
                            entryType = doc.getString("type") ?: "PHOTO",
                            ageAtCreation = "", 
                            emotionalCategory = "",
                            visibility = "",
                            mediaUrl = doc.getString("mediaUrl")
                        )
                    }
                }

                entry?.let { resolvedMedia[mediaId] = it }
            } catch (e: Exception) {
                android.util.Log.w("PHOENX_BOOK", "Impossible de résoudre le média $mediaId")
            }
        }
        _mediaMap.value = resolvedMedia
    }
}
