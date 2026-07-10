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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(YoungSelfLetterUiState())
    val uiState: StateFlow<YoungSelfLetterUiState> = _uiState.asStateFlow()

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

    fun saveLetter(onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val state = _uiState.value
        val birthDate = state.birthDate ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val encryptedPayload = encryptionManager.encryptText(state.letterContent)
                
                val ageAtCreation = AgeUtils.calculateAge(birthDate)
                val ageJson = "{\"years\":${ageAtCreation.years},\"months\":${ageAtCreation.months},\"days\":${ageAtCreation.days}}"

                /* // ANCIEN CODE - DESACTIVE 2024-05-24 : écriture manuelle Firestore, remplacée par le pipeline standard
                val entryData = hashMapOf(
                    "type" to "TEXT",
                    "encryptedContent" to android.util.Base64.encodeToString(encryptedPayload, android.util.Base64.DEFAULT),
                    "isYoungSelfLetter" to true,
                    "targetAge" to state.targetAge,
                    "ageAtCreation" to ageJson,
                    "emotionalCategory" to "Sagesse",
                    "isFulfilled" to false,
                    "aiSummary" to "",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("users").document(userId).collection("entries").add(entryData).await()
                */
                
                offlineEntryDao.insertEntry(
                    OfflineEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        encryptedPayload = encryptedPayload,
                        entryType = "TEXT",
                        ageAtCreation = ageJson,
                        emotionalCategory = "Sagesse",
                        visibility = "private",
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
    val isSaving: Boolean = false
) {
    val calculatedYear: Int get() = birthYear + targetAge
}
