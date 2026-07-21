package com.example.phoenx.ui.screens.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.model.BookChapter
import com.example.phoenx.data.model.BookDraft
import com.example.phoenx.data.model.BookStatus
import com.example.phoenx.data.model.ChapterStatus
import com.example.phoenx.service.BookGeneratorService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await as kotlinAwait
import javax.inject.Inject

@HiltViewModel
class BookEditorViewModel @Inject constructor(
    private val bookService: BookGeneratorService,
    private val auth: FirebaseAuth,
    private val encryptionManager: EncryptionManager,
    private val offlineEntryDao: com.example.phoenx.data.local.OfflineEntryDao
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft

    private val _decryptedContents = MutableStateFlow<Map<String, String>>(emptyMap())
    val decryptedContents: StateFlow<Map<String, String>> = _decryptedContents

    private val _selectedChapter = MutableStateFlow<BookChapter?>(null)
    val selectedChapter: StateFlow<BookChapter?> = _selectedChapter

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

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
            _bookDraft.value = draft
            if (draft != null) {
                decryptAllChapters(userId, draft)
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
        val current = _bookDraft.value ?: return
        val userId = auth.currentUser?.uid ?: return
        
        // Mise à jour de la version déchiffrée en mémoire (UI)
        val updatedMap = _decryptedContents.value.toMutableMap()
        updatedMap[chapterId] = newContent
        _decryptedContents.value = updatedMap

        viewModelScope.launch {
            try {
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
                android.util.Log.e("BookEditorVM", "Erreur lors du chiffrement et sauvegarde")
            }
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
                updateChapterContent(
                    chapterId,
                    newContent
                )
            } catch (e: Exception) {
                _error.value = "L'IA n'a pas pu modifier ce chapitre."
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
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            bookService.saveBookDraft(userId, updated)
        }
    }

    fun updateRecipients(recipientIds: List<String>) {
        val current = _bookDraft.value ?: return
        val updated = current.copy(recipientIds = recipientIds)
        _bookDraft.value = updated
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            bookService.saveBookDraft(userId, updated)
        }
    }

    fun updateSealedMessage(message: String) {
        val current = _bookDraft.value ?: return
        val updated = current.copy(sealedMessage = message)
        _bookDraft.value = updated
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            bookService.saveBookDraft(userId, updated)
        }
    }
}
