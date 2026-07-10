package com.example.phoenx.ui.screens.questions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.domain.util.AgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuestionsUiState>(QuestionsUiState())
    val uiState: StateFlow<QuestionsUiState> = _uiState

    val questions = listOf(
        "Quel est le repas que tu aimerais manger une dernière fois ?",
        "Quelle décision as-tu prise dont tu es le plus fier ?",
        "Qu'est-ce que tu aurais voulu qu'on te dise à 20 ans ?",
        "Qui t'a le plus appris sans jamais te faire cours ?",
        "Si tu pouvais revivre une journée, laquelle ?",
        "Quelle est la chose dont tu parles le moins mais qui compte le plus ?",
        "Quel est ton plus beau souvenir de voyage ?",
        "Quelle est ta plus grande peur aujourd'hui ?",
        "Quel est le trait de caractère que tu préfères chez toi ?",
        "Qu'est-ce qui te fait rire à coup sûr ?"
    )

    fun saveAnswer(question: String, answer: String) {
        val user = auth.currentUser ?: return
        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                
                val encrypted = encryptionManager.encryptText(answer)
                
                val entry = OfflineEntry(
                    encryptedPayload = encrypted,
                    entryType = "QUESTION_ANSWER",
                    ageAtCreation = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }",
                    emotionalCategory = "Sagesse",
                    visibility = "private",
                    createdAt = System.currentTimeMillis(),
                    aiSummary = "Réponse à : $question"
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
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}

data class QuestionsUiState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false
)
