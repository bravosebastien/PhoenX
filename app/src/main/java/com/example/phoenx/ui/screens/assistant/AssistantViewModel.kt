package com.example.phoenx.ui.screens.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.preferences.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val auth: com.google.firebase.auth.FirebaseAuth // v9.4.25
) : ViewModel() {

    val bubbleX: StateFlow<Float?> = preferenceManager.assistantBubbleX
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bubbleY: StateFlow<Float?> = preferenceManager.assistantBubbleY
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // v9.6.7 : Gestion du surnom de l'assistant
    private val _nickname = MutableStateFlow<String?>(null)
    val nickname: StateFlow<String?> = _nickname.asStateFlow()
    
    private val _showNicknameDialog = MutableStateFlow(false)
    val showNicknameDialog: StateFlow<Boolean> = _showNicknameDialog.asStateFlow()

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    init {
        loadNickname()
    }

    private fun loadNickname() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                val savedNickname = doc.getString("assistantNickname")
                _nickname.value = savedNickname
            } catch (e: Exception) {
                android.util.Log.e("AssistantVM", "Erreur chargement surnom: ${e.message}")
            }
        }
    }

    fun saveNickname(newNickname: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("assistantNickname", newNickname)
                    .await()
                _nickname.value = newNickname
                _showNicknameDialog.value = false
            } catch (e: Exception) {
                android.util.Log.e("AssistantVM", "Erreur sauvegarde surnom: ${e.message}")
            }
        }
    }

    fun dismissNicknameDialog() {
        _showNicknameDialog.value = false
    }

    fun toggleChat() {
        _isChatOpen.value = !_isChatOpen.value
        // Au premier clic, si pas de surnom, on demande
        if (_isChatOpen.value && _nickname.value == null) {
            _showNicknameDialog.value = true
        }
    }

    val suggestedQuestions = listOf(
        "Comment déposer mon premier souvenir ?",
        "Qui pourra voir ce que j'écris ?",
        "Comment marche la sécurisation ?"
    )

    fun injectSystemMessage(text: String) {
        if (_chatMessages.value.none { !it.isUser && it.text == text }) {
            _chatMessages.update { it + ChatMessage(text, isUser = false) }
        }
    }

    fun savePosition(x: Float, y: Float) {
        viewModelScope.launch {
            preferenceManager.saveAssistantBubblePosition(x, y)
        }
    }

    fun askQuestion(question: String) {
        if (question.isBlank()) return
        
        // v9.6.7 : Utilisation du surnom si disponible, repli sur displayName
        val userName = _nickname.value ?: auth.currentUser?.displayName ?: "Utilisateur"

        val userMsg = ChatMessage(question, isUser = true)
        _chatMessages.update { it + userMsg }
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
                val params = mapOf(
                    "question" to question,
                    "userName" to userName
                )
                val result = functions.getHttpsCallable("askAssistant")
                    .call(params)
                    .await()
                
                val data = result.data as? Map<*, *>
                val answer = data?.get("answer") as? String ?: "Désolé, je n'ai pas pu obtenir de réponse."
                
                _chatMessages.update { it + ChatMessage(answer, isUser = false) }
            } catch (e: Exception) {
                _chatMessages.update { it + ChatMessage("Une erreur est survenue.", isUser = false) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }
}
