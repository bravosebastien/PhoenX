package com.example.phoenx.ui.screens.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.model.BookDraft
import com.example.phoenx.service.BookGeneratorService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BookViewerViewModel — Gère l'affichage du livre scellé.
 */
@HiltViewModel
class BookViewerViewModel @Inject constructor(
    private val bookService: BookGeneratorService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBook()
    }

    fun loadBook() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    _bookDraft.value = bookService.loadBookDraft(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_BOOK", "Erreur chargement livre: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
