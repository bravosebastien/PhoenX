package com.example.phoenx.ui.screens.youngselfletters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import android.content.Context
import androidx.work.*
import com.example.phoenx.data.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class YoungSelfLetterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val encryptionManager: EncryptionManager,
    private val offlineEntryDao: OfflineEntryDao,
    private val analyticsTracker: com.example.phoenx.data.analytics.AnalyticsTracker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(YoungSelfLetterUiState())
    val uiState: StateFlow<YoungSelfLetterUiState> = _uiState.asStateFlow()

    val existingLetters: StateFlow<List<OfflineEntry>> = offlineEntryDao.getAllEntries()
        .map { entries -> entries.filter { it.isYoungSelfLetter } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipients = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadUserBirthYear()
    }

    fun loadUserBirthYear() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                val birthTimestamp = doc.getTimestamp("dateOfBirth")
                birthTimestamp?.let { ts ->
                    val birthDate = ts.toDate()
                    val birthYear = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                    _uiState.update { it.copy(birthYear = birthYear, birthDate = birthDate) }
                }
            } catch (e: Exception) {
                android.util.Log.e("YoungSelfLetterVM", "Error loading birth year", e)
            }
        }
    }

    fun updateContent(text: String) {
        _uiState.update { it.copy(letterContent = text) }
    }

    fun updateTargetAge(age: Int) {
        _uiState.update { it.copy(targetAge = age) }
    }

    fun getSuggestions() {
        val targetAge = _uiState.value.targetAge
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestions = true) }
            try {
                val allEntries = offlineEntryDao.getAllEntriesSync()
                val summariesAtThatAge = allEntries.filter { 
                    AgeUtils.parseAgeJson(it.ageAtCreation).years in (targetAge - 2)..(targetAge + 2)
                }.map { it.aiSummary }.filter { it.isNotEmpty() }

                val data = hashMapOf(
                    "targetAge" to targetAge,
                    "summariesAtThatAge" to summariesAtThatAge
                )

                val result = functions.getHttpsCallable("generateYoungSelfSuggestions")
                    .call(data)
                    .await()
                
                val response = result.data as? String ?: ""
                val suggestionList = response.split("\n").filter { it.isNotBlank() }.map { it.trim().removePrefix("- ") }
                
                _uiState.update { it.copy(suggestions = suggestionList, isLoadingSuggestions = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSuggestions = false) }
                android.util.Log.e("YoungSelfLetterVM", "Error getting suggestions", e)
            }
        }
    }

    fun toggleRecipient(id: String) {
        _uiState.update { state ->
            val current = state.selectedRecipientIds
            val newList = if (current.contains(id)) current.filter { it != id } else current + id
            state.copy(selectedRecipientIds = newList.distinct())
        }
    }

    fun setKeepPrivate(private: Boolean) {
        _uiState.update { it.copy(keepPrivate = private, includeInBook = if (private) false else it.includeInBook) }
    }

    fun setVisibility(vis: String) {
        _uiState.update { it.copy(visibility = vis) }
    }

    fun setIncludeInBook(include: Boolean) {
        _uiState.update { it.copy(includeInBook = include) }
    }

    fun saveLetter(onSuccess: () -> Unit) {
        val userId = auth.currentUser ?: return
        val state = _uiState.value
        val birthDate = state.birthDate ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val encryptedPayload = encryptionManager.encryptText(state.letterContent)
                
                val ageAtCreation = AgeUtils.calculateAge(birthDate)
                val ageJson = "{\"years\":${ageAtCreation.years},\"months\":${ageAtCreation.months},\"days\":${ageAtCreation.days}}"

                offlineEntryDao.insertEntry(
                    OfflineEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        creatorUid = userId.uid, // FIX: Toujours spécifier l'UID (v8.6.2)
                        encryptedPayload = encryptedPayload,
                        entryType = "TEXT",
                        ageAtCreation = ageJson,
                        emotionalCategory = "Sagesse",
                        visibility = if (state.keepPrivate) "private" else if (state.visibility == "EVERYONE") "EVERYONE" else "specific",
                        recipientIds = if (state.keepPrivate) "" else state.selectedRecipientIds.distinct().joinToString(","),
                        includeInBook = (!state.keepPrivate && state.includeInBook),
                        isYoungSelfLetter = true,
                        targetAge = state.targetAge,
                        syncStatus = "pending"
                    )
                )

                // DECLENCHEMENT PIPELINE STANDARD (SyncWorker)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)

                try {
                    analyticsTracker.logMemoryCreated(source = "lettre")
                    val rootCount = offlineEntryDao.getAllEntriesSync().count { it.parentEntryId.isNullOrBlank() }
                    if (rootCount == 1 || rootCount == 3 || rootCount == 10) {
                        analyticsTracker.logMemoryMilestone(rootCount)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("YoungSelfLetterVM", "Erreur analytics memory_created", e)
                }

                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                android.util.Log.e("YoungSelfLetterVM", "Error saving letter", e)
            }
        }
    }
}

data class YoungSelfLetterUiState(
    val letterContent: String = "",
    val targetAge: Int = 20,
    val birthYear: Int = 1990,
    val birthDate: Date? = null,
    val suggestions: List<String> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val isSaving: Boolean = false,
    val keepPrivate: Boolean = true,
    val selectedRecipientIds: List<String> = emptyList(),
    val visibility: String = "specific",
    val includeInBook: Boolean = false
) {
    val calculatedYear: Int get() = birthYear + targetAge
}
