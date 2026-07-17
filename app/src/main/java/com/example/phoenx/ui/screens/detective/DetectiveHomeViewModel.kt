package com.example.phoenx.ui.screens.detective

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DetectiveHomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao
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
                // Filtrage local (Room) — ADN 5.0 / v8.3
                offlineEntryDao.getAllEntries().collect { allEntries ->
                    _entries.value = allEntries.filter { it.enigmaQuestion != null }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("DetectiveHomeVM", "Erreur chargement local", e)
                _isLoading.value = false
            }
        }
    }
}
