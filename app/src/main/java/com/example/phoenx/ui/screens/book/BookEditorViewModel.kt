package com.example.phoenx.ui.screens.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.model.*
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft

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

    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val draft = bookService.loadBookDraft(userId)
            
            if (draft != null) {
                // v9.2.1 : On affiche d'abord le livre brut pour éviter le blocage (réinstallation)
                _bookDraft.value = draft
                decryptAllChapters(userId, draft)

                if (draft.globalIntroduction.isNotEmpty()) {
                    val bookKey = bookService.getBookKey(userId)
                    _decryptedGlobalIntro.value = bookService.decryptChapter(draft.globalIntroduction, bookKey)
                }

                // Remappage des IDs pour l'UI en asynchrone dès que les destinataires sont là
                recipients.collect { allRecipients ->
                    if (allRecipients.isNotEmpty()) {
                        val mappedIds = draft.recipientIds.map { persistentId ->
                            allRecipients.find { it.linkedUid == persistentId }?.id ?: persistentId
                        }
                        _bookDraft.value = draft.copy(recipientIds = mappedIds)
                    }
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

    fun generateBook() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isGenerating.value = true
            _error.value = null
            try {
                val draft = bookService.generateBook(userId) { progress ->
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

    fun updateRecipients(selectedDocIds: List<String>) {
        val current = _bookDraft.value ?: return
        val allRecipients = recipients.value
        
        // v9.2 : On stocke les VRAIS UIDs pour la sécurité Firestore/Functions
        // Si un proche n'est pas encore lié (pas de linkedUid), on garde son DocID 
        // comme placeholder (il ne pourra pas lire tant qu'il n'est pas lié anyway).
        val persistentIds = selectedDocIds.map { docId ->
            allRecipients.find { it.id == docId }?.linkedUid ?: docId
        }

        val updated = current.copy(recipientIds = persistentIds)
        _bookDraft.value = updated
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isSaving.value = true
            try {
                bookService.saveBookDraft(userId, updated)
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur lors de la sauvegarde"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateSealedMessage(message: String) {
        val current = _bookDraft.value ?: return
        val updated = current.copy(sealedMessage = message)
        _bookDraft.value = updated
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isSaving.value = true
            try {
                bookService.saveBookDraft(userId, updated)
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur lors de la sauvegarde"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateBookTitle(title: String) {
        val current = _bookDraft.value ?: return
        val updated = current.copy(bookTitle = title.ifBlank { null })
        _bookDraft.value = updated
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isSaving.value = true
            try {
                bookService.saveBookDraft(userId, updated)
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

    fun updateTheme(backgroundId: String, fontId: String) {
        val current = _bookDraft.value ?: return
        val userId = auth.currentUser?.uid ?: return
        val updatedTheme = BookTheme(backgroundId, fontId)
        val updatedDraft = current.copy(theme = updatedTheme)
        _bookDraft.value = updatedDraft
        viewModelScope.launch {
            _isSaving.value = true
            try {
                bookService.saveBookDraft(userId, updatedDraft)
                triggerSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur sauvegarde thème"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateCoverImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val current = _bookDraft.value ?: return

        viewModelScope.launch {
            _isSaving.value = true
            try {
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
                val updatedDraft = current.copy(coverImageUrl = downloadUrl)
                _bookDraft.value = updatedDraft
                bookService.saveBookDraft(userId, updatedDraft)
                
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

    private fun triggerSuccess() {
        viewModelScope.launch {
            _saveSuccess.value = true
            kotlinx.coroutines.delay(2000)
            _saveSuccess.value = false
        }
    }
}
