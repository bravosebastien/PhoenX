package com.example.phoenx.ui.screens.depositary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class RedeemState {
    object Loading : RedeemState()
    object Success : RedeemState()
    data class Error(val message: String) : RedeemState()
}

@HiltViewModel
class DepositaryViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepositaryUiState())
    val uiState: StateFlow<DepositaryUiState> = _uiState.asStateFlow()

    private val _activationSuccess = MutableSharedFlow<Boolean>()
    val activationSuccess: SharedFlow<Boolean> = _activationSuccess.asSharedFlow()

    private val _joinSuccess = MutableSharedFlow<Boolean>()
    val joinSuccess: SharedFlow<Boolean> = _joinSuccess.asSharedFlow()

    private val _redeemState = MutableStateFlow<RedeemState>(RedeemState.Loading)
    val redeemState: StateFlow<RedeemState> = _redeemState.asStateFlow()

    fun redeemShortCode(shortCode: String) {
        _redeemState.value = RedeemState.Loading
        viewModelScope.launch {
            try {
                // 1. Échange du code court contre le token
                val result = functions.getHttpsCallable("redeemDepositaryShortCode")
                    .call(mapOf("shortCode" to shortCode))
                    .await()
                
                val data = result.data as Map<*, *>
                val creatorId = data["creatorId"] as String
                val depositaryId = data["depositaryId"] as String
                val token = data["token"] as String
                
                // 2. Liaison effective du compte
                val joinData = hashMapOf(
                    "creatorId" to creatorId,
                    "depositaryId" to depositaryId,
                    "token" to token
                )
                functions.getHttpsCallable("joinAsDepositary")
                    .call(joinData)
                    .await()
                
                _redeemState.value = RedeemState.Success
                _joinSuccess.emit(true)
                
            } catch (e: Exception) {
                val message = if (e is FirebaseFunctionsException) {
                    when (e.code) {
                        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                            "Ce lien d'invitation a expiré. Demande un nouveau lien à ton proche."
                        FirebaseFunctionsException.Code.ALREADY_EXISTS ->
                            "Ce lien a déjà été utilisé. Si c'est toi qui l'as ouvert précédemment, continue normalement."
                        FirebaseFunctionsException.Code.NOT_FOUND ->
                            "Ce lien est invalide. Vérifie le lien reçu par email."
                        else ->
                            "Une erreur est survenue. Réessaie dans quelques instants."
                    }
                } else {
                    "Une erreur de connexion est survenue. Vérifie ton accès internet."
                }
                _redeemState.value = RedeemState.Error(message)
                android.util.Log.e("PHOENX_AUTH", "Erreur rachat code: ${e.message}")
            }
        }
    }

    // Gardé pour compatibilité ou appel direct si besoin
    fun joinAsDepositary(creatorId: String, depositaryId: String, token: String) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "creatorId" to creatorId,
                    "depositaryId" to depositaryId,
                    "token" to token
                )
                functions.getHttpsCallable("joinAsDepositary")
                    .call(data)
                    .await()
                
                _joinSuccess.emit(true)
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_AUTH", "Erreur liaison Dépositaire: ${e.message}")
            }
        }
    }

    fun loadCreatorStatus(creatorId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(creatorId).get().await()
                val name = doc.getString("displayName") ?: "Proche"
                val missedCycles = doc.get("silenceConfig.missedCycles")?.toString()?.toInt() ?: 0
                val lastCheckInAt = doc.getTimestamp("silenceConfig.lastCheckInAt")
                
                val daysSince = if (lastCheckInAt != null) {
                    (System.currentTimeMillis() - lastCheckInAt.toDate().time) / (1000 * 60 * 60 * 24)
                } else 0
                
                _uiState.update { it.copy(
                    creatorName = name,
                    missedCycles = missedCycles,
                    daysSinceLastCheckIn = daysSince.toInt(),
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resolveAlert(creatorId: String, depositaryId: String, note: String?) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "creatorId" to creatorId,
                    "depositaryId" to depositaryId,
                    "note" to note
                )
                functions.getHttpsCallable("resolveCreatorSilence")
                    .call(data)
                    .await()

                loadCreatorStatus(creatorId)
            } catch (e: Exception) {
                android.util.Log.e("PHOENX_ALERT", "Erreur résolution alerte: ${e.message}")
            }
        }
    }

    fun activateProtocol(
        creatorId: String,
        depositaryId: String,
        contactAttemptNote: String,
        contactAttemptDetails: Map<String, Boolean>,
        depositaryNote: String?
    ) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "creatorId" to creatorId,
                    "depositaryId" to depositaryId,
                    "contactAttemptNote" to contactAttemptNote,
                    "contactAttemptDetails" to contactAttemptDetails,
                    "depositaryNote" to depositaryNote
                )
                functions.getHttpsCallable("activateProtocol")
                    .call(data)
                    .await()
                
                _activationSuccess.emit(true)
            } catch (e: Exception) {
                // Gérer l'erreur
            }
        }
    }
}

data class DepositaryUiState(
    val creatorName: String = "",
    val missedCycles: Int = 0,
    val daysSinceLastCheckIn: Int = 0,
    val isLoading: Boolean = true
)
