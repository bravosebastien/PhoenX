package com.example.phoenx.ui.screens.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.model.*
import com.example.phoenx.data.preferences.PreferenceManager
import com.example.phoenx.service.BookGeneratorService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await as kotlinAwait
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@HiltViewModel
class BookEditorViewModel @Inject constructor(
    private val bookService: BookGeneratorService,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: com.example.phoenx.data.local.OfflineEntryDao,
    private val mediaManager: MediaManager,
    private val preferenceManager: PreferenceManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)

    private val _decryptedContents = MutableStateFlow<Map<String, String>>(emptyMap())
    val decryptedContents: StateFlow<Map<String, String>> = _decryptedContents

    private val _decryptedGlobalIntro = MutableStateFlow("")
    val decryptedGlobalIntro: StateFlow<String> = _decryptedGlobalIntro

    private val _selectedChapter = MutableStateFlow<BookChapter?>(null)
    val selectedChapter: StateFlow<BookChapter?> = _selectedChapter

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _isGeneratingGlobalIntro = MutableStateFlow(false)
    val isGeneratingGlobalIntro: StateFlow<Boolean> = _isGeneratingGlobalIntro

    private val _generationProgress = MutableStateFlow("")
    val generationProgress: StateFlow<String> = _generationProgress

    private val _proposedPlan = MutableStateFlow<List<Map<String, Any?>>?>(null)
    val proposedPlan: StateFlow<List<Map<String, Any?>>?> = _proposedPlan

    private val _isModifyingWithAi = MutableStateFlow(false)
    val isModifyingWithAi: StateFlow<Boolean> = _isModifyingWithAi

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isUserCreator = MutableStateFlow<Boolean?>(null)
    val isUserCreator: StateFlow<Boolean?> = _isUserCreator

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    // AMBIANCE GLOBALE v9.4.27
    private val _globalAmbiance = MutableStateFlow(com.example.phoenx.ui.screens.recipient.AmbianceState())
    val globalAmbiance: StateFlow<com.example.phoenx.ui.screens.recipient.AmbianceState> = _globalAmbiance.asStateFlow()

    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // v9.4.22 : Version réactive mappée pour l'UI (UIDs -> DocIDs)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft.combine(recipients) { draft, allRecipients ->
        if (draft == null || allRecipients.isEmpty()) draft
        else {
            val mappedIds = draft.recipientIds.map { persistentId ->
                allRecipients.find { it.linkedUid == persistentId }?.id ?: persistentId
            }.distinct()
            draft.copy(recipientIds = mappedIds)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedRecipientIds: StateFlow<List<String>> = bookDraft
        .map { it?.recipientIds ?: emptyList() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entryCount: StateFlow<Int> = offlineEntryDao.getAllEntries()
        .map { entries -> entries.filter { it.parentEntryId == null }.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        checkCreatorStatus()
        loadExistingBook()
    }

    private fun checkCreatorStatus() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(userId).get().kotlinAwait()
                _isUserCreator.value = doc.getBoolean("isCreator") ?: true
                _userName.value = doc.getString("displayName") ?: "Votre proche"
            } catch (e: Exception) {
                _isUserCreator.value = true
                _userName.value = "Votre proche"
            }
        }
    }

    fun loadExistingBook() {
        android.util.Log.d("PHOENX_BOOK_TRACE", "A. Début loadExistingBook (Éditeur)")
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            // 1. Charger l'ambiance globale (v9.4.27)
            offlineEntryDao.getCreatorProfile(userId).collect { profile ->
                if (profile != null) {
                    _globalAmbiance.value = com.example.phoenx.ui.screens.recipient.AmbianceState(
                        backgroundId = profile.transmissionBackgroundId,
                        fontId = profile.transmissionFontId
                    )
                }
            }
        }
        
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val draft = bookService.loadBookDraft(userId)
            android.util.Log.d("PHOENX_BOOK_TRACE", "B. Résultat service: ${if (draft == null) "NULL" else "PRÉSENT (" + draft.chapters.size + " chapitres)"}")
            
            if (draft != null) {
                // v9.2.1 : On affiche d'abord le livre brut pour éviter le blocage (réinstallation)
                _bookDraft.value = draft
                decryptAllChapters(userId, draft)

                if (draft.globalIntroduction.isNotEmpty()) {
                    val bookKey = bookService.getBookKey(userId)
                    _decryptedGlobalIntro.value = bookService.decryptChapter(draft.globalIntroduction, bookKey)
                }
            } else {
                _bookDraft.value = null
            }
        }
    }

    private suspend fun decryptAllChapters(userId: String, draft: BookDraft) {
        val bookKey = bookService.getBookKey(userId)
        val decrypted = draft.chapters.associate { it.id to bookService.decryptChapter(it.content, bookKey) }
        _decryptedContents.value = decrypted
    }

    fun generateBook(plan: List<Map<String, Any?>>? = null) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isGenerating.value = true
            _error.value = null
            _proposedPlan.value = null // On efface le plan proposé une fois validé
            try {
                val draft = bookService.generateBook(userId, plan) { progress ->
                    _generationProgress.value = progress
                }
                _bookDraft.value = draft
                decryptAllChapters(userId, draft)
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur lors de la génération."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun proposePlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _generationProgress.value = "Ébauche de la structure de votre vie..."
            try {
                val plan = bookService.generateBookPlan()
                _proposedPlan.value = plan
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur lors de la création du plan."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun cancelPlan() {
        _proposedPlan.value = null
    }

    fun selectChapter(chapter: BookChapter?) {
        _selectedChapter.value = chapter
    }

    fun updateChapterContent(chapterId: String, newContent: String) {
        viewModelScope.launch {
            try {
                saveChapterInternal(chapterId, newContent)
            } catch (e: Exception) {
                // L'erreur est logguée dans saveChapterInternal
            }
        }
    }

    private suspend fun saveChapterInternal(chapterId: String, newContent: String) {
        val current = _bookDraft.value ?: return
        val userId = auth.currentUser?.uid ?: return

        try {
            // v8.6.4 : Mise à jour immédiate de la mémoire déchiffrée (UI)
            val updatedMap = _decryptedContents.value.toMutableMap()
            updatedMap[chapterId] = newContent
            _decryptedContents.value = updatedMap

            val bookKey = bookService.getBookKey(userId)
            val encryptedContent = bookService.encryptChapter(newContent, bookKey)

            val updatedDraft = current.copy(
                chapters = current.chapters.map { chapter ->
                    if (chapter.id == chapterId)
                        chapter.copy(
                            content = encryptedContent,
                            status = ChapterStatus.IN_REVIEW
                        )
                    else chapter
                }
            )
            _bookDraft.value = updatedDraft
            bookService.saveBookDraft(userId, updatedDraft)
        } catch (e: Exception) {
            android.util.Log.e("BookEditorVM", "Erreur lors du chiffrement et sauvegarde: ${e.message}")
            throw e
        }
    }

    fun askAiToModify(chapterId: String, instruction: String) {
        viewModelScope.launch {
            _isModifyingWithAi.value = true
            val decryptedContent = _decryptedContents.value[chapterId] ?: return@launch
            try {
                // Pour modification IA on utilise le texte en clair (déjà déchiffré dans le VM)
                val newContent = bookService.askAiToModifyChapter(
                    currentContent = decryptedContent,
                    instruction = instruction
                )
                saveChapterInternal(
                    chapterId,
                    newContent
                )
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur IA : ${e.message ?: "Cause inconnue"}"
            } finally {
                _isModifyingWithAi.value = false
            }
        }
    }

    fun validateChapter(chapterId: String) {
        val current = _bookDraft.value ?: return
        val updated = current.copy(
            chapters = current.chapters.map { chapter ->
                if (chapter.id == chapterId)
                    chapter.copy(status = ChapterStatus.VALIDATED)
                else chapter
            }
        )
        _bookDraft.value = updated
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            bookService.saveBookDraft(userId, updated)
        }
    }

    fun unvalidateChapter(chapterId: String) {
        val current = _bookDraft.value ?: return
        val updated = current.copy(
            chapters = current.chapters.map { chapter ->
                if (chapter.id == chapterId)
                    chapter.copy(status = ChapterStatus.IN_REVIEW)
                else chapter
            }
        )
        _bookDraft.value = updated
        
        // v8.7.1 : Mise à jour du chapitre sélectionné pour débloquer l'UI
        updated.chapters.find { it.id == chapterId }?.let {
            _selectedChapter.value = it
        }

        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            bookService.saveBookDraft(userId, updated)
        }
    }

    fun updateRecipients(selectedDocIds: List<String>, visibility: String? = null) {
        val userId = auth.currentUser?.uid ?: return
        val allRecipients = recipients.value
        
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // v9.4.29 : Fresh Read avant modification
                val freshDraft = bookService.loadBookDraft(userId) ?: _bookDraft.value ?: BookDraft(userId = userId)
                
                // v9.2 : On stocke les VRAIS UIDs pour la sécurité Firestore/Functions
                val persistentIds = selectedDocIds.map { docId ->
                    allRecipients.find { it.id == docId }?.linkedUid ?: docId
                }.distinct()

                // v9.4.27 : La visibilité ne change que si explicitement demandée. 
                // Si la liste est vide, on reste en RESTRICTED par sécurité.
                val finalVisibility = visibility ?: freshDraft.visibility ?: "RESTRICTED"

                val updated = freshDraft.copy(
                    recipientIds = persistentIds,
                    visibility = finalVisibility
                )
                
                bookService.saveBookDraft(userId, updated)
                _bookDraft.value = updated
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur lors de la sauvegarde"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Alterne la sélection d'un destinataire (v9.4.19)
     */
    fun toggleRecipient(docId: String) {
        val current = selectedRecipientIds.value
        val newList = if (current.contains(docId)) {
            current.filter { it != docId }
        } else {
            current + docId
        }
        updateRecipients(newList, _bookDraft.value?.visibility ?: "RESTRICTED")
    }

    fun updateSealedMessage(message: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // v9.4.29 : Fresh Read avant modification
                val freshDraft = bookService.loadBookDraft(userId) ?: _bookDraft.value ?: BookDraft(userId = userId)
                val updated = freshDraft.copy(sealedMessage = message)
                
                bookService.saveBookDraft(userId, updated)
                _bookDraft.value = updated
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur lors de la sauvegarde"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateBookTitle(title: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // v9.4.29 : Fresh Read avant modification
                val freshDraft = bookService.loadBookDraft(userId) ?: _bookDraft.value ?: BookDraft(userId = userId)
                val updated = freshDraft.copy(bookTitle = title.ifBlank { null })
                
                bookService.saveBookDraft(userId, updated)
                _bookDraft.value = updated // Synchro UI
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur lors de la sauvegarde"
            } finally {
                _isSaving.value = false
            }
        }
    }

    // --- v8.7.0 : ÉVOLUTIONS LECTURE CONTINUE ---

    fun generateGlobalIntro() {
        val current = _bookDraft.value ?: return
        auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isGeneratingGlobalIntro.value = true
            try {
                val chapterTitles = current.chapters.sortedBy { it.orderIndex }.map { it.title }
                val content = bookService.generateGlobalIntro(chapterTitles)
                updateGlobalIntro(content)
            } catch (e: Exception) {
                _error.value = "Erreur génération intro : ${e.message}"
            } finally {
                _isGeneratingGlobalIntro.value = false
            }
        }
    }

    fun updateGlobalIntro(newContent: String) {
        val current = _bookDraft.value ?: return
        val userId = auth.currentUser?.uid ?: return
        
        _decryptedGlobalIntro.value = newContent

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val bookKey = bookService.getBookKey(userId)
                val encrypted = bookService.encryptChapter(newContent, bookKey)
                val updatedDraft = current.copy(globalIntroduction = encrypted)
                _bookDraft.value = updatedDraft
                bookService.saveBookDraft(userId, updatedDraft)
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur sauvegarde intro"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Met à jour l'ambiance de transmission au niveau GLOBAL (v9.4.27)
     * Applique le principe de Fresh Read pour protéger les autres champs du profil.
     */
    fun updateGlobalAmbiance(backgroundId: String, fontId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // 0. MISE À JOUR PRÉFÉRENCES LOCALES (v9.4.29 : Rétablissement réactivité thème)
                preferenceManager.setGlobalTheme(backgroundId, fontId)

                // 1. FRESH READ de la base locale (Garantie de sécurité Lot 3)
                val currentLocal = offlineEntryDao.getCreatorProfileSync(userId) 
                    ?: com.example.phoenx.data.local.CreatorProfileEntity(userId = userId)
                
                // 2. Préparation de la fusion : On ne change QUE l'ambiance
                val finalToSave = currentLocal.copy(
                    transmissionBackgroundId = backgroundId,
                    transmissionFontId = fontId,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "pending"
                )

                // 3. Sauvegarde locale (Room)
                offlineEntryDao.insertCreatorProfile(finalToSave)

                // 4. Sauvegarde Firestore PARTIELLE (Point 2 Lot 2)
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .update(
                        "transmissionBackgroundId", backgroundId,
                        "transmissionFontId", fontId
                    ).kotlinAwait()
                
                // 5. Marquage synchro
                offlineEntryDao.insertCreatorProfile(finalToSave.copy(syncStatus = "synced"))
                
                // 6. Synchronisation du thème du draft actuel (v9.4.27 : MISE À JOUR PARTIELLE CIBLÉE)
                // v9.4.29 : Fresh Read Firestore avant de toucher au draft
                val freshDraft = bookService.loadBookDraft(userId)
                
                freshDraft?.let { currentDraft ->
                    // v9.4.29: Le thème du draft lui-même ne stocke pas showPersonPhotos (réglage global user)
                    val updatedThemeMap = mapOf(
                        "backgroundId" to backgroundId,
                        "fontId" to fontId
                    )
                    
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .collection("book").document("current_draft")
                        .update("theme", updatedThemeMap).kotlinAwait()
                    
                    // Mise à jour de l'état local pour l'UI
                    _bookDraft.value = currentDraft.copy(theme = BookTheme(backgroundId, fontId))
                }

                triggerSuccess()
            } catch (e: Exception) {
                android.util.Log.e("BookEditorVM", "Erreur sauvegarde ambiance globale", e)
                _error.value = "Erreur sauvegarde ambiance"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateTheme(backgroundId: String, fontId: String) {
        // Redirection vers la nouvelle logique globale (v9.4.27)
        updateGlobalAmbiance(backgroundId, fontId)
    }

    fun updateCoverImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // v9.4.29 : Fresh Read avant modification
                val freshDraft = bookService.loadBookDraft(userId) ?: _bookDraft.value ?: BookDraft(userId = userId)

                // 1. Conversion Uri -> File temporaire
                val tempFile = File(context.cacheDir, "book_cover_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 2. Upload vers Storage (On réutilise le dossier cameos pour la simplicité v9.2.4)
                val downloadUrl = mediaManager.uploadCameo(userId, "book_cover", tempFile)
                
                // 3. Mise à jour du Draft
                val updatedDraft = freshDraft.copy(coverImageUrl = downloadUrl)
                bookService.saveBookDraft(userId, updatedDraft)
                _bookDraft.value = updatedDraft
                
                tempFile.delete()
                triggerSuccess()
            } catch (e: Exception) {
                android.util.Log.e("BookEditorVM", "Erreur upload couverture: ${e.message}")
                _error.value = "Erreur lors de l'envoi de l'image"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateCoverTitleStyle(style: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // v9.4.29 : Fresh Read avant modification
                val freshDraft = bookService.loadBookDraft(userId) ?: _bookDraft.value ?: BookDraft(userId = userId)
                val updated = freshDraft.copy(coverTitleStyle = style)
                
                bookService.saveBookDraft(userId, updated)
                _bookDraft.value = updated
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur sauvegarde style titre"
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun triggerSuccess() {
        viewModelScope.launch {
            _saveSuccess.value = true
            kotlinx.coroutines.delay(2000)
            _saveSuccess.value = false
        }
    }
}
