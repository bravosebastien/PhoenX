package com.example.phoenx.ui.screens.detective

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DetectiveHomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _entries = MutableStateFlow<List<OfflineEntry>>(emptyList())
    val entries: StateFlow<List<OfflineEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDetectiveEntries()
    }

    fun loadDetectiveEntries() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users")
                    .document(userId)
                    .collection("entries")
                    .whereEqualTo("isDetective", true)
                    .get()
                    .await()
                
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(OfflineEntry::class.java)?.copy(id = doc.id)
                }
                _entries.value = list
            } catch (e: Exception) {
                android.util.Log.e("DetectiveHomeVM", "Erreur chargement", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
