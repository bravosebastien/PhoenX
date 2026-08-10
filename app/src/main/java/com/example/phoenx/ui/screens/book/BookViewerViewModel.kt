package com.example.phoenx.ui.screens.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.media.MediaManager
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
    val mediaManager: MediaManager,
    private val offlineEntryDao: OfflineEntryDao,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft.asStateFlow()

    private val _decryptedChapters = MutableStateFlow<Map<String, String>>(emptyMap())
    val decryptedChapters: StateFlow<Map<String, String>> = _decryptedChapters.asStateFlow()

    private val _decryptedGlobalIntro = MutableStateFlow("")
    val decryptedGlobalIntro: StateFlow<String> = _decryptedGlobalIntro.asStateFlow()

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

    // v8.7.0 : Progrès de lecture et Confort
    private val _readingProgress = MutableStateFlow<ReadingPosition?>(null)
    val readingProgress: StateFlow<ReadingPosition?> = _readingProgress.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(1.0f)
    val fontSizeScale: StateFlow<Float> = _fontSizeScale.asStateFlow()

    private val _forcedAmbiance = MutableStateFlow<com.example.phoenx.ui.screens.recipient.AmbianceState?>(null)
    val forcedAmbiance: StateFlow<com.example.phoenx.ui.screens.recipient.AmbianceState?> = _forcedAmbiance.asStateFlow()

    fun loadBook(
        targetCreatorId: String? = null,
        simulatedRecipientUid: String? = null,
        ambiance: com.example.phoenx.ui.screens.recipient.AmbianceState? = null
    ) {
        android.util.Log.d("PHOENX_BOOK_TRACE", "A. ViewModel.loadBook entré")
        android.util.Log.d("PHOENX_BOOK_TRACE", "   - targetCreatorId: $targetCreatorId")
        android.util.Log.d("PHOENX_BOOK_TRACE", "   - simulatedRecipientUid: $simulatedRecipientUid")
        android.util.Log.d("PHOENX_BOOK_TRACE", "   - currentAuthUid: ${auth.currentUser?.uid}")

        viewModelScope.launch {
            _isLoading.value = true
            _forcedAmbiance.value = ambiance
            try {
                // v9.4.27 : Déduction de l'UID du créateur dont on lit le livre
                val userId = targetCreatorId ?: auth.currentUser?.uid
                if (userId == null) {
                    android.util.Log.e("PHOENX_BOOK_TRACE", "B. ABANDON : userId du créateur non trouvé")
                    return@launch
                }

                // Charger le progrès de lecture en parallèle (v8.7.0)
                loadReadingProgress(userId)

                // 1. VÉRIFICATION DE SÉCURITÉ
                // v9.4.27 : Court-circuit si mode APERÇU (simulatedRecipientUid != null)
                if (targetCreatorId != null && simulatedRecipientUid == null) {
                    android.util.Log.d("PHOENX_BOOK_TRACE", "C1. Mode VRAI DESTINATAIRE (Protocole)")
                    try {
                        val result = functions.getHttpsCallable("getCreatorBookStatus")
                            .call(mapOf("creatorId" to targetCreatorId))
                            .kotlinAwait()
                        
                        val data = result.data as Map<*, *>
                        _creatorName.value = data["displayName"] as? String ?: "Ton proche"
                        val isBookOpen = data["isBookOpen"] as? Boolean ?: false
                        _sealedMessage.value = data["sealedMessage"] as? String
                        
                        android.util.Log.d("PHOENX_BOOK_TRACE", "C2. Réponse Cloud - isBookOpen: $isBookOpen")

                        if (!isBookOpen) {
                            android.util.Log.w("PHOENX_BOOK_TRACE", "C3. Livre encore SCELLÉ pour le destinataire.")
                            _isLocked.value = true
                            _isLoading.value = false
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PHOENX_BOOK_TRACE", "C3. ERREUR Cloud ou Accès refusé", e)
                        _isLocked.value = true
                        _isLoading.value = false
                        return@launch
                    }
                } else if (simulatedRecipientUid != null) {
                    android.util.Log.d("PHOENX_BOOK_TRACE", "C1. Mode APERÇU SIMULÉ (Créateur teste)")
                    _isLocked.value = false
                    try {
                        val userDoc = db.collection("users").document(userId).get().kotlinAwait()
                        _creatorName.value = userDoc.getString("displayName") ?: "Moi"
                    } catch (e: Exception) {
                        _creatorName.value = "Moi"
                    }
                } else {
                    android.util.Log.d("PHOENX_BOOK_TRACE", "C1. Mode CRÉATEUR CLASSIQUE (Direct)")
                    try {
                        val userDoc = db.collection("users").document(userId).get().kotlinAwait()
                        _creatorName.value = userDoc.getString("displayName") ?: "Moi"
                    } catch (e: Exception) {
                        _creatorName.value = "Moi"
                    }
                }

                _isLocked.value = false
                android.util.Log.d("PHOENX_BOOK_TRACE", "D. Appel service loadBookDraft pour $userId")
                val draft = bookService.loadBookDraft(userId)
                android.util.Log.d("PHOENX_BOOK_TRACE", "E. Résultat draft: ${if (draft == null) "NULL" else "PRÉSENT (" + draft.chapters.size + " chapitres)"}")
                _bookDraft.value = draft

                if (draft != null) {
                    // v9.4.27 : On passe l'UID du destinataire (réel ou simulé) pour filtrer les médias
                    val effectiveRecipientUid = simulatedRecipientUid ?: (if (targetCreatorId != null) auth.currentUser?.uid else null)
                    android.util.Log.d("PHOENX_BOOK_TRACE", "F. Début résolution médias. RecipientUid effectif: $effectiveRecipientUid")
                    decryptAndResolveMedia(userId, draft, effectiveRecipientUid)
                }

            } catch (e: Exception) {
                android.util.Log.e("PHOENX_BOOK_TRACE", "ERREUR CRITIQUE fatale dans loadBook", e)
            } finally {
                _isLoading.value = false
                android.util.Log.d("PHOENX_BOOK_TRACE", "Z. Fin chargement, isLoading=false")
            }
        }
    }

    private suspend fun decryptAndResolveMedia(userId: String, draft: BookDraft, recipientUid: String? = null) {
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

        // 2. Déchiffrement de l'intro globale (v8.7.0)
        if (draft.globalIntroduction.isNotEmpty()) {
            _decryptedGlobalIntro.value = encryptionManager.decrypt(draft.globalIntroduction, bookKey)
        }

        // 3. Déchiffrement des chapitres
        draft.chapters.forEach { chapter ->
            val decrypted = encryptionManager.decrypt(chapter.content, bookKey)
            chapterContents[chapter.id] = decrypted
            
            // Extraction des IDs média [PHOTO:uuid] ou [AUDIO:uuid]
            val regex = Regex("\\[(PHOTO|AUDIO):([a-f0-9-]+)]")
            regex.findAll(decrypted).forEach { match ->
                mediaIds.add(match.groupValues[2])
            }
        }
        _decryptedChapters.value = chapterContents

        // 4. Résolution des médias
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
                            entryType = doc.getString("entryType") ?: "PHOTO", // Fix: entryType dans Firestore
                            ageAtCreation = "", 
                            emotionalCategory = "",
                            visibility = doc.getString("visibility") ?: "RESTRICTED",
                            recipientIds = (doc.get("recipientIds") as? List<*>)?.joinToString(",") ?: "",
                            mediaUrl = doc.getString("mediaUrl")
                        )
                    }
                }

                // 5. FILTRAGE DE SÉCURITÉ (v9.4.27)
                if (entry != null) {
                    val isVisible = entry.visibility == "EVERYONE" || 
                        (recipientUid != null && entry.recipientIds.split(",").map { it.trim() }.contains(recipientUid))
                    
                    if (isVisible || recipientUid == null) {
                        resolvedMedia[mediaId] = entry
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("PHOENX_BOOK", "Impossible de résoudre le média $mediaId")
            }
        }
        _mediaMap.value = resolvedMedia
    }

    // --- v8.7.0 : GESTION DU MARQUE-PAGE & CONFORT ---

    fun loadReadingProgress(creatorId: String) {
        val readerId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(readerId)
                    .collection("reading_progress").document("${creatorId}_book").get().kotlinAwait()
                
                if (doc.exists()) {
                    _readingProgress.value = ReadingPosition(
                        itemIndex = doc.getLong("itemIndex")?.toInt() ?: 0,
                        offset = doc.getLong("offset")?.toInt() ?: 0,
                        savedAtScale = doc.getDouble("savedAtScale")?.toFloat() ?: 1.0f
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_READING", "Erreur chargement progrès")
            }
        }
    }

    fun saveReadingProgress(creatorId: String, index: Int, offset: Int) {
        val readerId = auth.currentUser?.uid ?: return
        val currentScale = _fontSizeScale.value
        viewModelScope.launch {
            try {
                db.collection("users").document(readerId)
                    .collection("reading_progress").document("${creatorId}_book")
                    .set(mapOf(
                        "itemIndex" to index,
                        "offset" to offset,
                        "savedAtScale" to currentScale,
                        "timestamp" to System.currentTimeMillis()
                    )).kotlinAwait()
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_READING", "Erreur sauvegarde progrès")
            }
        }
    }

    fun updateFontSize(scale: Float) {
        _fontSizeScale.value = scale.coerceIn(0.8f, 1.5f)
    }
}

data class ReadingPosition(
    val itemIndex: Int,
    val offset: Int,
    val savedAtScale: Float
)
