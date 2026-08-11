package com.example.phoenx.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.data.model.Quiz
import com.example.phoenx.data.model.QuizQuestion
import com.example.phoenx.data.model.QuizResult
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.domain.util.EnigmaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    val mediaManager: MediaManager
) : ViewModel() {

    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    private val _currentQuiz = MutableStateFlow<Quiz?>(null)
    val currentQuiz: StateFlow<Quiz?> = _currentQuiz.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _answers = MutableStateFlow<List<String>>(emptyList())
    val answers: StateFlow<List<String>> = _answers.asStateFlow()

    private val _helpUsed = MutableStateFlow(false)
    val helpUsed: StateFlow<Boolean> = _helpUsed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _results = MutableStateFlow<List<QuizResult>>(emptyList())
    val results: StateFlow<List<QuizResult>> = _results.asStateFlow()

    private val _userResult = MutableStateFlow<QuizResult?>(null)
    val userResult: StateFlow<QuizResult?> = _userResult.asStateFlow()

    private val _recipients = MutableStateFlow<List<com.example.phoenx.data.local.RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = _recipients.asStateFlow()

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    init {
        loadRecipients()
    }

    fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collect { list ->
                _recipients.value = list
            }
        }
    }

    fun loadQuizzes(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("quizzes")
                    .get().await()
                val list = snapshot.toObjects(Quiz::class.java)
                _quizzes.value = list
            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Error loading quizzes", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadQuiz(creatorId: String, quizId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Charger la clé héritage (v8.3 Support Héritage)
                // RÈGLE : Le Quiz utilise la clé miroir générale (entry_keys/main)
                var explicitKey: ByteArray? = null
                if (currentUserId != creatorId) {
                    try {
                        val keyDoc = db.collection("users").document(creatorId)
                            .collection("entry_keys").document("main").get().await()
                        
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            explicitKey = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                            _heirKey.value = explicitKey
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("QuizVM", "Impossible de récupérer la clé héritage")
                    }
                }

                // 2. Charger le Quiz (MAPPING MANUEL v9.4.27 : Zéro dépendance toObject)
                val doc = db.collection("users").document(creatorId)
                    .collection("quizzes").document(quizId)
                    .get().await()
                
                val raw = doc.data ?: throw Exception("Quiz introuvable")
                
                // Reconstruction robuste des questions
                val rawQuestions = raw["questions"] as? List<*>
                val questions = rawQuestions?.mapNotNull { item ->
                    val q = item as? Map<*, *> ?: return@mapNotNull null
                    QuizQuestion(
                        id = q["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                        text = q["text"] as? String ?: "",
                        mediaUrl = q["mediaUrl"] as? String,
                        mediaType = q["mediaType"] as? String,
                        correctAnswer = q["correctAnswer"] as? String ?: "",
                        correctHash = q["correctHash"] as? String ?: "",
                        distractors = (q["distractors"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        teasingMessages = (q["teasingMessages"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        difficultyAllowed = q["difficultyAllowed"] as? Boolean ?: true
                    )
                } ?: emptyList()

                val quiz = Quiz(
                    id = doc.id,
                    title = raw["title"] as? String ?: "Quiz",
                    isActive = raw["isActive"] as? Boolean ?: true,
                    questions = questions,
                    finalMessage = raw["finalMessage"] as? String ?: ""
                )
                
                // 3. Déchiffrement des données sensibles (v8.3)
                val decryptedQuestions = quiz.questions.map { q ->
                    val decryptedAnswer = if (q.correctAnswer.isNotEmpty()) {
                        try {
                            encryptionManager.decryptText(
                                android.util.Base64.decode(q.correctAnswer, android.util.Base64.DEFAULT),
                                explicitKey
                            )
                        } catch (e: Exception) { q.correctAnswer }
                    } else ""
                    q.copy(correctAnswer = decryptedAnswer)
                }

                val decFinalMsg = try {
                    if (quiz.finalMessage.isNotEmpty()) {
                        encryptionManager.decryptText(
                            android.util.Base64.decode(quiz.finalMessage, android.util.Base64.DEFAULT),
                            explicitKey
                        )
                    } else ""
                } catch (e: Exception) { quiz.finalMessage }
                
                _currentQuiz.value = quiz.copy(
                    questions = decryptedQuestions,
                    finalMessage = decFinalMsg
                )

                // 4. ISOLATION RÉQUETE RÉSULTATS ( try-catch indépendant v9.4.27)
                try {
                    val resultSnap = db.collection("users").document(creatorId)
                        .collection("quizResults")
                        .whereEqualTo("recipientId", currentUserId)
                        .get().await()
                    
                    if (!resultSnap.isEmpty) {
                        val r = resultSnap.documents.first()
                        _userResult.value = QuizResult(
                            score = (r.get("score") as? Number)?.toInt() ?: 0,
                            totalQuestions = (r.get("totalQuestions") as? Number)?.toInt() ?: 0
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("QuizVM", "Échec chargement score précédent")
                }

            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Erreur critique chargement quiz", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun answerQuestion(answer: String, usedHelp: Boolean = false) {
        val quiz = _currentQuiz.value ?: return
        val currentQuestion = quiz.questions.getOrNull(_currentQuestionIndex.value) ?: return

        val hashedInput = EnigmaUtils.hashAnswer(answer)
        if (currentQuestion.correctHash == hashedInput) {
            val points = if (usedHelp) 1 else 3 // Hard mode rapporte plus
            _score.update { it + points }
        }

        if (usedHelp) _helpUsed.value = true
        _answers.update { it + answer }
        _currentQuestionIndex.update { it + 1 }
    }

    /**
     * Retourne les choix mélangés (Vrai + Distracteurs)
     */
    fun getDisplayChoices(question: QuizQuestion, correctAnswer: String): List<String> {
        return (question.distractors + correctAnswer).shuffled()
    }

    /**
     * Pioche un message de chambrage aléatoire
     */
    fun getRandomTeasing(question: QuizQuestion): String {
        if (question.teasingMessages.isEmpty()) return "Pas mal, mais j'ai connu mieux !"
        return question.teasingMessages.random()
    }

    /**
     * Appelle l'IA pour générer 3 distracteurs cohérents (v8.3 Quiz 2.0)
     */
    fun generateDistractorsForQuestion(
        questionText: String,
        correctAnswer: String,
        onResult: (List<String>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "question" to questionText,
                    "correctAnswer" to correctAnswer
                )
                
                val result = com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("generateDistractors")
                    .call(data)
                    .await()
                
                val response = result.data as Map<*, *>
                val distractors = response["distractors"] as? List<String> ?: emptyList()
                onResult(distractors)
            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Erreur génération distracteurs", e)
                onResult(emptyList())
            }
        }
    }

    fun submitResult(creatorId: String, quizId: String, recipientName: String) {
        val user = auth.currentUser ?: return
        val quiz = _currentQuiz.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = QuizResult(
                    recipientId = user.uid,
                    recipientName = if (quiz.showNames) recipientName else null,
                    score = _score.value,
                    totalQuestions = quiz.questions.size,
                    answers = _answers.value,
                    helpUsed = _helpUsed.value
                )
                
                db.collection("users").document(creatorId)
                    .collection("quizResults")
                    .add(result).await()
                
                _userResult.value = result
            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Error submitting result", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadResults(creatorId: String, quizId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(creatorId)
                    .collection("quizResults")
                    .orderBy("score", Query.Direction.DESCENDING)
                    .get().await()
                val list = snapshot.toObjects(QuizResult::class.java)
                _results.value = list
            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Error loading results", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveQuiz(quiz: Quiz) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // v9.2.2 : Remappage DocID -> UID pour la sécurité Firestore
                val persistentRecipientIds = quiz.recipientIds.map { docId ->
                    _recipients.value.find { it.id == docId }?.linkedUid ?: docId
                }

                // Chiffrement des éléments sensibles (v8.3)
                val processedQuestions = quiz.questions.map { q ->
                    q.copy(
                        correctAnswer = android.util.Base64.encodeToString(
                            encryptionManager.encryptText(q.correctAnswer),
                            android.util.Base64.DEFAULT
                        )
                    )
                }

                val encryptedMessage = if (quiz.finalMessage.isNotEmpty()) {
                    android.util.Base64.encodeToString(
                        encryptionManager.encryptText(quiz.finalMessage),
                        android.util.Base64.DEFAULT
                    )
                } else ""

                val quizToSave = quiz.copy(
                    questions = processedQuestions,
                    finalMessage = encryptedMessage,
                    recipientIds = persistentRecipientIds
                )
                
                // On force l'ID à 'main_quiz' pour faciliter l'accès héritier (v8.6.2)
                db.collection("users").document(userId)
                    .collection("quizzes").document("main_quiz")
                    .set(quizToSave).await()
            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Error saving quiz", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
