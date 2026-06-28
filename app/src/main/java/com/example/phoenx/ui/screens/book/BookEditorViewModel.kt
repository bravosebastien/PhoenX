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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookEditorViewModel @Inject constructor(
    private val bookService: BookGeneratorService,
    private val auth: FirebaseAuth,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft

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

    init {
        loadExistingBook()
    }

    fun loadExistingBook() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _bookDraft.value = bookService.loadBookDraft(userId)
        }
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
        val updated = current.copy(
            chapters = current.chapters.map { chapter ->
                if (chapter.id == chapterId)
                    chapter.copy(
                        content = newContent,
                        status = ChapterStatus.IN_REVIEW
                    )
                else chapter
            }
        )
        _bookDraft.value = updated
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            bookService.saveBookDraft(userId, updated)
        }
    }

    fun askAiToModify(chapterId: String, instruction: String) {
        viewModelScope.launch {
            _isModifyingWithAi.value = true
            val chapter = _bookDraft.value
                ?.chapters
                ?.find { it.id == chapterId }
                ?: return@launch
            try {
                // Pour modification IA on déchiffre d'abord
                val currentContent = chapter.content
                val newContent = bookService.askAiToModifyChapter(
                    currentContent = currentContent,
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
}
