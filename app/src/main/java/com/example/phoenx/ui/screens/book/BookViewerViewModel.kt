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
import kotlinx.coroutines.tasks.await as kotlinAwait
import javax.inject.Inject

/**
 * BookViewerViewModel — Gère l'affichage du livre scellé.
 */
@HiltViewModel
class BookViewerViewModel @Inject constructor(
    private val bookService: BookGeneratorService,
    private val auth: FirebaseAuth,
    private val functions: com.google.firebase.functions.FirebaseFunctions
) : ViewModel() {

    private val _bookDraft = MutableStateFlow<BookDraft?>(null)
    val bookDraft: StateFlow<BookDraft?> = _bookDraft.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

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
                _bookDraft.value = bookService.loadBookDraft(userId)
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_BOOK", "Erreur chargement livre: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
