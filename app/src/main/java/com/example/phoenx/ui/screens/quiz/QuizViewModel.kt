package com.example.phoenx.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.model.Quiz
import com.example.phoenx.data.model.QuizQuestion
import com.example.phoenx.data.model.QuizResult
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
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    private val _currentQuiz = MutableStateFlow<Quiz?>(null)
    val currentQuiz: StateFlow<Quiz?> = _currentQuiz.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _answers = MutableStateFlow<List<Int>>(emptyList())
    val answers: StateFlow<List<Int>> = _answers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _results = MutableStateFlow<List<QuizResult>>(emptyList())
    val results: StateFlow<List<QuizResult>> = _results.asStateFlow()

    private val _userResult = MutableStateFlow<QuizResult?>(null)
    val userResult: StateFlow<QuizResult?> = _userResult.asStateFlow()

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
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = db.collection("users").document(creatorId)
                    .collection("quizzes").document(quizId)
                    .get().await()
                val quiz = doc.toObject(Quiz::class.java)
                
                // Déchiffrer le message final si présent
                val finalMessage = quiz?.finalMessage?.let {
                    if (it.isNotEmpty()) {
                        try {
                            val decrypted = encryptionManager.decryptText(android.util.Base64.decode(it, android.util.Base64.DEFAULT))
                            decrypted
                        } catch (e: Exception) { it }
                    } else it
                }
                
                _currentQuiz.value = quiz?.copy(finalMessage = finalMessage ?: "")

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

    fun answerQuestion(answerIndex: Int) {
        val quiz = _currentQuiz.value ?: return
        val currentQuestion = quiz.questions.getOrNull(_currentQuestionIndex.value) ?: return

        if (currentQuestion.correctIndex == answerIndex) {
            _score.update { it + currentQuestion.points }
        }

        _answers.update { it + answerIndex }
        _currentQuestionIndex.update { it + 1 }
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
                    answers = _answers.value
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
                // Chiffrer le message final
                val encryptedMessage = if (quiz.finalMessage.isNotEmpty()) {
                    android.util.Base64.encodeToString(
                        encryptionManager.encryptText(quiz.finalMessage),
                        android.util.Base64.DEFAULT
                    )
                } else ""

                val quizToSave = quiz.copy(finalMessage = encryptedMessage)
                
                if (quiz.id.isEmpty()) {
                    db.collection("users").document(userId)
                        .collection("quizzes")
                        .add(quizToSave).await()
                } else {
                    db.collection("users").document(userId)
                        .collection("quizzes").document(quiz.id)
                        .set(quizToSave).await()
                }
            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Error saving quiz", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
