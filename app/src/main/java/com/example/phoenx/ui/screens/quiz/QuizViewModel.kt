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
                // 1. Charger la clé de déchiffrement si on est un héritier (v8.3 Support Héritage)
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
                        android.util.Log.w("QuizVM", "Impossible de récupérer la clé héritage (protocole non activé ?)")
                    }
                }

                // 2. Charger le Quiz
                val doc = db.collection("users").document(creatorId)
                    .collection("quizzes").document(quizId)
                    .get().await()
                val quiz = doc.toObject(Quiz::class.java)
                
                // 3. Déchiffrement des données sensibles (v8.3)
                val decryptedQuestions = quiz?.questions?.map { q ->
                    val decryptedAnswer = if (q.correctAnswer.isNotEmpty()) {
                        try {
                            encryptionManager.decryptText(
                                android.util.Base64.decode(q.correctAnswer, android.util.Base64.DEFAULT),
                                explicitKey
                            )
                        } catch (e: Exception) { q.correctAnswer }
                    } else ""
                    q.copy(correctAnswer = decryptedAnswer)
                } ?: emptyList()

                val finalMessage = quiz?.finalMessage?.let {
                    if (it.isNotEmpty()) {
                        try {
                            encryptionManager.decryptText(
                                android.util.Base64.decode(it, android.util.Base64.DEFAULT),
                                explicitKey
                            )
                        } catch (e: Exception) { it }
                    } else it
                }
                
                _currentQuiz.value = quiz?.copy(
                    questions = decryptedQuestions,
                    finalMessage = finalMessage ?: ""
                )

                // Vérifier si l'utilisateur a déjà joué
                val recipientId = auth.currentUser?.uid ?: ""
                val resultSnap = db.collection("users").document(creatorId)
                    .collection("quizResults")
                    .whereEqualTo("recipientId", recipientId)
                    // .whereEqualTo("quizId", quizId) // On suppose un seul quiz par créateur pour l'instant ou on ajoute quizId au modèle
                    .get().await()
                
                // Comme on n'a pas quizId dans QuizResult (selon le prompt), on vérifie par ID ou on suppose un quiz unique
                val existingResult = resultSnap.toObjects(QuizResult::class.java).firstOrNull()
                _userResult.value = existingResult

            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Error loading quiz", e)
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
                    finalMessage = encryptedMessage
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
