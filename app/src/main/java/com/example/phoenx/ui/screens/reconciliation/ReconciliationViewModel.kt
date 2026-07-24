package com.example.phoenx.ui.screens.reconciliation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReconciliationViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    private val aiManager: AIManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReconciliationUiState>(ReconciliationUiState())
    val uiState: StateFlow<ReconciliationUiState> = _uiState

    fun getAIHelp(recipientName: String, intent: String) {
        _uiState.value = _uiState.value.copy(isLoadingHelp = true)
        viewModelScope.launch {
            try {
                val help = aiManager.generateReconciliationHelp(recipientName, intent)
                _uiState.value = _uiState.value.copy(aiHelp = help, isLoadingHelp = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingHelp = false)
            }
        }
    }

    fun saveReconciliationMessage(content: String, recipientName: String) {
        val user = auth.currentUser ?: return
        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                val encrypted = withContext(Dispatchers.Default) {
                    encryptionManager.encryptText(content)
                }
                
                val entry = OfflineEntry(
                    encryptedPayload = encrypted,
                    entryType = "RECONCILIATION",
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = "Amour",
                    visibility = "specific",
                    createdAt = System.currentTimeMillis(),
                    aiSummary = "Message de réconciliation pour $recipientName"
                )
                offlineEntryDao.insertEntry(entry)

                // DECLENCHEMENT PIPELINE STANDARD (SyncWorker)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)
                
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}

data class ReconciliationUiState(
    val aiHelp: String? = null,
    val isLoadingHelp: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
