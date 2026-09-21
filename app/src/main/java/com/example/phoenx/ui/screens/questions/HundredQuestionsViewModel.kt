package com.example.phoenx.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.sync.SyncWorker
import com.example.phoenx.data.sync.toOfflineEntry
import com.example.phoenx.domain.util.AgeUtils
import com.example.phoenx.domain.util.EnigmaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import java.util.Date
import javax.inject.Inject
import android.content.Context
import com.example.phoenx.R
import dagger.hilt.android.qualifiers.ApplicationContext

data class HundredQuestionsUiState(
    val questions: List<Question> = emptyList(),
    val answeredQuestionIds: Set<String> = emptySet(),
    val customQuestions: List<OfflineEntry> = emptyList(),
    val selectedCategory: String = "Toutes",
    val isLoading: Boolean = false,
    // v12.7.7 : Champs pour le mode Destinataire
    val creatorName: String = "",
    val lockedQuestions: List<OfflineEntry> = emptyList(),
    val unlockedQuestionIds: Set<String> = emptySet(),
    val attempts: Map<String, Int> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class HundredQuestionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: com.google.firebase.functions.FirebaseFunctions,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    val mediaManager: MediaManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HundredQuestionsUiState())
    val uiState: StateFlow<HundredQuestionsUiState> = _uiState.asStateFlow()

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAnsweredQuestions()
        loadCustomQuestions()
        observeLocalBadges()
        filterQuestions("Toutes")
    }

    private fun observeLocalBadges() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collect { entries ->
                val localAnsweredIds = entries.mapNotNull { it.questionId }.toSet()
                _uiState.update { it.copy(answeredQuestionIds = it.answeredQuestionIds + localAnsweredIds) }
            }
        }
    }

    private fun loadCustomQuestions() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collect { entries ->
                val custom = entries.filter { it.isGuessQuestion && it.questionId.isNullOrBlank() }
                _uiState.update { it.copy(customQuestions = custom) }
            }
        }
    }

    fun loadAnsweredQuestions() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Charger les questionId déjà répondus depuis Firestore
                val snapshot = db.collection("users").document(userId)
                    .collection("entries")
                    .whereNotEqualTo("questionId", null)
                    .get().await()
                
                val ids = snapshot.documents.mapNotNull { it.getString("questionId") }.toSet()
                _uiState.update { it.copy(answeredQuestionIds = ids, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun filterQuestions(category: String) {
        val filtered = if (category == "Toutes" || category == "Mes Questions") {
            QuestionsData.allQuestions
        } else {
            QuestionsData.allQuestions.filter { it.category == category }
        }
        _uiState.update { it.copy(questions = filtered, selectedCategory = category) }
    }

    fun saveCustomQuestion(
        id: String? = null,
        question: String,
        hint: String?,
        answer: String,
        visibility: String = "EVERYONE",
        recipientIds: String = "",
        answerType: String = "WORD",
        expectedWordCount: Int? = null
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Résolution immédiate des UIDs (v12.7.8)
                val allRecipients = recipients.value
                val resolvedIds = recipientIds.split(",")
                    .filter { it.isNotBlank() }
                    .map { rid -> allRecipients.find { it.id == rid }?.linkedUid ?: rid }
                    .distinct()
                    .joinToString(",")

                // 2. CALCUL DE L'ÂGE
                val userDoc = db.collection("users").document(userId).get().await()
                val birthDate = userDoc.getTimestamp("dateOfBirth")?.toDate() ?: Date()
                val age = AgeUtils.calculateAge(birthDate)
                val ageJson = "{ \"years\": ${age.years}, \"months\": ${age.months}, \"days\": ${age.days} }"

                val existingEntry = id?.let { offlineEntryDao.getEntryById(it).firstOrNull() }
                val hashedAnswer: String?
                val plainAnswer: String?
                if (answer.isNotBlank()) {
                    hashedAnswer = EnigmaUtils.hashAnswer(answer, answerType)
                    plainAnswer = answer
                } else {
                    hashedAnswer = existingEntry?.enigmaAnswer
                    plainAnswer = existingEntry?.enigmaAnswerPlain
                }
                val finalAnswerType = if (answer.isNotBlank()) answerType else (existingEntry?.answerType ?: answerType)

                // Pour les 100 questions, le "récit" est vide car il n'y a rien à débloquer (v12.7.7)
                val encryptedPayload = encryptionManager.encryptText("")

                val entry = OfflineEntry(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    creatorUid = userId,
                    encryptedPayload = encryptedPayload,
                    entryType = "QUESTION_ANSWER", // v12.7.7 : Type explicite
                    ageAtCreation = ageJson,
                    emotionalCategory = "Sagesse",
                    visibility = visibility,
                    recipientIds = resolvedIds,
                    enigmaQuestion = question,
                    enigmaAnswer = hashedAnswer,
                    enigmaAnswerPlain = plainAnswer,
                    enigmaHint = hint,
                    isGuessQuestion = true,
                    userTitle = question,
                    syncStatus = "pending",
                    answerType = finalAnswerType,
                    expectedWordCount = expectedWordCount
                )
                
                offlineEntryDao.insertEntry(entry)
                
                // Déclenchement sync
                SyncWorker.trigger(context)
            } catch (e: Exception) {
                android.util.Log.e("HundredQuestionsVM", context.getString(R.string.questions_error_custom_save), e)
            }
        }
    }

    fun deleteCustomQuestion(entryId: String) {
        viewModelScope.launch {
            offlineEntryDao.deleteEntry(entryId)
        }
    }

    private var recipientDataJob: Job? = null

    /**
     * v12.7.8 : Charge les questions d'un créateur (Mode Destinataire)
     * Version réactive avec logging explicite des erreurs Firestore.
     */
    fun loadRecipientData(creatorId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        
        // Annuler toute exécution précédente pour éviter les "Job was cancelled" multiples (v12.7.8b)
        recipientDataJob?.cancel()
        
        recipientDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Infos Créateur
                var name = "Ton proche"
                try {
                    val creatorDoc = db.collection("users").document(creatorId).get().await()
                    name = creatorDoc.getString("displayName") ?: "Ton proche"
                    _uiState.update { it.copy(creatorName = name) }
                } catch (e: Exception) {
                    android.util.Log.w("PHOENX_DEBUG", "Permission Denied sur le profil: ${e.message}")
                }

                // 2. Clé Miroir (Chargement initial indispensable pour déchiffrer les questions)
                try {
                    val keyDoc = db.collection("users").document(creatorId)
                        .collection("entry_keys").document("main").get().await()
                    val keyBase64 = keyDoc.getString("key")
                    if (keyBase64 != null) {
                        _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PHOENX_DEBUG", "Erreur chargement clé miroir: ${e.message}")
                }

                // 3. Double Flux Réactif (Public + Privé) avec Logging d'Erreur (v12.7.8)
                val publicFlow = callbackFlow {
                    val listener = db.collection("users").document(creatorId)
                        .collection("entries")
                        .whereEqualTo("visibility", "EVERYONE")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("PHOENX_DEBUG", "Erreur Firestore 100 Questions (Public): ${error.message}", error)
                            }
                            val items = snapshot?.documents
                                ?.mapNotNull { it.toOfflineEntry(encryptionManager, _heirKey.value) }
                                ?.filter { it.entryType == "QUESTION_ANSWER" || it.isGuessQuestion }
                                ?: emptyList()
                            trySend(items)
                        }
                    awaitClose { listener.remove() }
                }

                val privateFlow = callbackFlow {
                    val listener = db.collection("users").document(creatorId)
                        .collection("entries")
                        .whereArrayContains("recipientIds", currentUid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("PHOENX_DEBUG", "Erreur Firestore 100 Questions (Privé): ${error.message}", error)
                            }
                            val items = snapshot?.documents
                                ?.mapNotNull { it.toOfflineEntry(encryptionManager, _heirKey.value) }
                                ?.filter { it.entryType == "QUESTION_ANSWER" || it.isGuessQuestion }
                                ?: emptyList()
                            trySend(items)
                        }
                    awaitClose { listener.remove() }
                }

                combine(publicFlow, privateFlow) { pub, priv ->
                    (pub + priv).distinctBy { it.id }
                }.collect { entries ->
                    _uiState.update { it.copy(lockedQuestions = entries, isLoading = false) }
                }

            } catch (e: CancellationException) {
                // Annulation normale par le ViewModel ou navigation
                android.util.Log.d("PHOENX_DEBUG", "loadRecipientData annulé normalement")
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_DEBUG", "Erreur critique loadRecipientData: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Erreur de connexion") }
            }
        }
    }

    fun attemptUnlock(entry: OfflineEntry, answer: String, creatorId: String) {
        val hashedInput = EnigmaUtils.hashAnswer(answer, entry.answerType)
        val isCorrect = entry.enigmaAnswer == hashedInput || entry.fallbackAnswer == hashedInput
        
        val newAttempts = _uiState.value.attempts.toMutableMap()
        val count = (newAttempts[entry.id] ?: 0) + 1
        newAttempts[entry.id] = count

        if (isCorrect) {
            val newUnlocked = _uiState.value.unlockedQuestionIds + entry.id
            _uiState.update { it.copy(unlockedQuestionIds = newUnlocked, attempts = newAttempts, error = null) }
            submitGuessResult(creatorId, entry.id, answer, count)
        } else {
            _uiState.update { it.copy(error = context.getString(R.string.detective_viewmodel_error_wrong_answer), attempts = newAttempts) }
        }
    }

    private fun submitGuessResult(creatorId: String, entryId: String, answer: String, attemptCount: Int) {
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("submitGuessResult")
                    .call(mapOf(
                        "creatorId" to creatorId,
                        "entryId" to entryId,
                        "answer" to answer,
                        "attemptCount" to attemptCount
                    )).await()
                android.util.Log.d("PHOENX_DEBUG", "submitGuessResult success: ${result.data}")
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_DEBUG", "submitGuessResult error: ${e.message}", e)
                _uiState.update { it.copy(error = "Erreur de synchro réseau : ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
